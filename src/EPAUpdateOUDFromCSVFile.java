
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;


public class EPAUpdateOUDFromCSVFile<E> {

	private static Properties configs;
	private static HashMap<String,String> attrNamesMap ;
	private static final String COMMASEPERATEDATTRIBUTENAMES = "COMMASEPERATEDATTRIBUTENAMES";
	
	private static StringBuilder errorLogger =  new StringBuilder();
	
	public static void main(String args[]){
		try {
			DirContext ctx = getDirectoryContext(configs.getProperty("oud.username"), configs.getProperty("oud.password"), configs.getProperty("oud.host"), configs.getProperty("oud.port"));
			List<HashMap<String,String>> data =  ReadFromFile.listUsers(""+System.getProperty("user.dir")+"/"+configs.getProperty("file.name"), ",", attrNamesMap);
			updateLdap(ctx, attrNamesMap, data);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	static {
		try {
			String propFileName = System.getProperty("user.dir")+"/config.properties";
			System.out.println("Loading configuration from : "+propFileName);
			configs = new Properties();
			configs.load(new FileInputStream(propFileName));
			if(!((String)configs.get("COMMASEPERATEDATTRIBUTENAMES")).split(",")[0].equalsIgnoreCase((String) configs.get("oud.key")))
			{
				System.out.println("Warning !!! First Attribute of the csv file and oud.key should be same, Found it diffrent. Existing the process. Please check 'oud.key' and 'COMMASEPERATEDATTRIBUTENAMES' in config file.");
				System.exit(0);
			}
			System.out.println("Running in Read Only mode : "+configs.getProperty("util.readonly")+"\nInput file Name : "+configs.getProperty("file.name")+"\nKey attribute: "+configs.get("oud.key") +"\nFile Headers Expected : "+configs.get("COMMASEPERATEDATTRIBUTENAMES")+" \nTo change any configuration "
					+ "Check following configuration file "+propFileName);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	static{
		attrNamesMap = new HashMap<String,String>();
		String tmp = configs.getProperty(COMMASEPERATEDATTRIBUTENAMES);
		if(null == tmp)
		{
			throw new NullPointerException("Ldap Util Failed, Comma Seperated Attribute Names should be provided in properties file.  ");
		}
		String[] tmpArr= tmp.split(",");
		int i =1;
		for(String attrName : tmpArr)
		{
			
			attrNamesMap.put(i+"", attrName);
			i++;
		}
	}
	
	public static void updateLdap(DirContext ctx,HashMap<String,String> attrNamesMap, List<HashMap<String,String>> data)
	{
		String filter = "("+configs.getProperty("oud.searchattributename")+"="+")";
		OIDUtil oidUtils = new OIDUtil();
		NamingEnumeration<SearchResult> userDetail = null;
    	int readonlywarning = 0;

		
		for(HashMap<String,String> row : data)
		{
			filter = "("+configs.getProperty("oud.searchattributename")+"="+row.get(1+"")+")";
			/**
			 * Fetch User DN from OUD
			 */
			try { 
				userDetail = oidUtils.searchOIDUsers(filter,ctx);
			
				String dn = "uid=kreddy,dc=pra,dc=com";
				while (userDetail.hasMore()) {
					SearchResult searchResult = (SearchResult) userDetail.next();
					Attributes attributes = searchResult.getAttributes();

					Attribute attr = attributes.get("orclnormdn");
					if (null != attr) {
						dn = ("" + attr.get()).replace("\\", "\\");
					}
				}
			
			/**
			 * Prepare attributes to be updated
			 */
			
            BasicAttributes basicAttrs = new BasicAttributes();
            if(null != dn)
            {
            	errorLogger.append(row.get(1+"") +","); 
            for (int i=2; i <= attrNamesMap.size(); i++)
            {
                basicAttrs.put(new BasicAttribute(attrNamesMap.get(i+""),row.get(i+"")));
                errorLogger.append(row.get(i+"")); 
                if(i <= attrNamesMap.size())
                errorLogger.append(",");
            }
            errorLogger.append("\n");


            /**
             * Update OUD Attributes
             */
            if(null != configs.get("util.readonly") &&  configs.get("util.readonly").equals("false"))
				ctx.modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, basicAttrs);
            else
            {
            	if(readonlywarning == 0)
            	{
	            	System.out.println("Running Utility in Read Mode Only util.readonly set  to : "+configs.get("util.readonly")+" set util.readonly to false for write mode.");
	            	readonlywarning =20;
            	}
            }
            }
            else{
                for (int i=1; i <= attrNamesMap.size(); i++)
                {
                    errorLogger.append(row.get(i+"")); 
                    errorLogger.append(",");
                }
                errorLogger.append("User Not found in directory"); 
                errorLogger.append("\n");
            }
            }
				catch (NamingException e) {
					errorLogger.append(e.getMessage());
					e.printStackTrace();

				}
			}
		String tmp = System.getProperty("user.dir")+"/report-"+(new Date()).getDate()+(new Date()).getTime()+".csv";
		ReadFromFile.usingBufferedWritter(tmp, errorLogger.toString());
        System.out.println("Success Report File Name :::  "+tmp);

		}
	
	private static DirContext getDirectoryContext(String username, String password,
			String host, String port) throws NamingException {

		Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		properties.put("com.sun.jndi.ldap.read.timeout", "5000");
		properties.put("java.naming.batchsize", "1000");
		properties.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port);
		properties.put(Context.SECURITY_AUTHENTICATION, "simple");
		properties.put(Context.SECURITY_PRINCIPAL, username);
		properties.put(Context.SECURITY_CREDENTIALS, password);

		// initializing active directory LDAP connection
		return new InitialLdapContext(properties, null);
	}
}
		
	

