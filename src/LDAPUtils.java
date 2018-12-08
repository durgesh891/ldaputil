
/**
 * @author Anuj Jain
 * 
 * @date Dec 05, 2016
 *
 * LDAPUtils.java
 * 
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

public class LDAPUtils {

	private String serverAddress;
	private String rootContext;
	private String principalFQDN;
	private String principalPassword;
	private int portNum;
	public static String searchBase = null;
	public static String adBase = null;
	public static String ldapParam = null;

	/**
	 * @param serverAddress
	 * @param rootContext
	 * @param principalFQDN
	 * @param principalPassword
	 * @param useSSL
	 * @param portNumStr
	 */
	public LDAPUtils(String serverAddress, String rootContext,
			String principalFQDN, String principalPassword, boolean useSSL,
			String portNumStr) {

		this.serverAddress = serverAddress;
		this.rootContext = rootContext;
		this.principalFQDN = principalFQDN;
		this.principalPassword = principalPassword;
		this.portNum = 386;

		if (!portNumStr.equalsIgnoreCase("")) {
			this.portNum = Integer.parseInt(portNumStr);
		}

	}

	/**
	 * @param serverAddress
	 * @param rootContext
	 * @param principalFQDN
	 * @param principalPassword
	 * @param useSSL
	 * @param portNumStr
	 */
	public LDAPUtils(String serverAddress, String rootContext,
			String principalFQDN, String principalPassword, String useSSL,
			String portNumStr) {

		this(serverAddress, rootContext, principalFQDN, principalPassword,
				false, portNumStr);

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public DirContext getLdapContext() throws Exception {

		Hashtable<String, String> env = new Hashtable<String, String>();
		String url = "ldap://" + serverAddress + ":" + portNum + "/"
				+ rootContext;

		// String url = "ldap://" + serverAddress + ":" + portNum + "/" +
		// rootContext;
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, principalFQDN);
		env.put(Context.SECURITY_CREDENTIALS, principalPassword);
		env.put(Context.REFERRAL, "follow");

		// LdapContext ldapCtx = new InitialLdapContext(env, null);
		DirContext dirContext = new InitialDirContext(env);
		// ctx = new InitialDirContext(env);
		return dirContext;
	}

	public static DirContext rootcreateLdapContext(String ldapHost,
			String ldapPort, String ldapPrincipal, String ldapPassword)
			throws NamingException {
		Hashtable env = new Hashtable();
		System.out.println("enters into createLdapContext method ");
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://" + ldapHost + ":" + ldapPort);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, ldapPrincipal);
		env.put(Context.SECURITY_CREDENTIALS, ldapPassword);

		return new InitialLdapContext(env, null);
	}

	/**
	 * @param ctx
	 * @param reqdAttrName
	 * @param searchAttrName
	 * @param searchAttrValue
	 * @return
	 * @throws Exception
	 */
	public List getSearchResults(DirContext ctx, String reqdAttrName,
			String searchAttrName, String searchAttrValue) throws Exception {

		System.out.println("AttributeName==" + reqdAttrName
				+ " searchAttributebName==" + searchAttrName
				+ " searchAttributebValue==" + searchAttrValue);

		List<String> matchingResults = new ArrayList<String>();

		SearchControls searchcontrols = new SearchControls();
		searchcontrols.setReturningAttributes(new String[] { reqdAttrName });
		searchcontrols.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String searchQuery = "(&(objectClass=*)(" + searchAttrName + "="
				+ searchAttrValue + "))";
		NamingEnumeration namingEnumeration = ctx.search(searchBase,
				searchQuery, searchcontrols);

		while (namingEnumeration.hasMoreElements()) {
			SearchResult searchresult = (SearchResult) namingEnumeration
					.nextElement();
			if (searchresult != null) {
				Attributes attrs = searchresult.getAttributes();
				if (attrs != null && attrs.size() != 0) {
					Attribute resultAttrValue = attrs.get(reqdAttrName);
					if (resultAttrValue != null) {
						String temp = (String) resultAttrValue.get();
						matchingResults.add(temp);
						System.out.println("Attribute value attributeValue="
								+ temp);
					}
				}

			}
		}
		return matchingResults;
	}

	public void assignLDAPRoles(DirContext ctx, String email, String role,
			DirContext rootContext) {
		String roleDn = getGroupDn(ctx, role);
		System.out.println("roleDn = " + roleDn);

		ModificationItem[] mods = new ModificationItem[1];
		String userDn = getUserDn(ctx, email);
		System.out.println("userDn = " + userDn);
		Attribute mod = new BasicAttribute("uniqueMember", userDn);
		mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, mod);
		try {
			rootContext.modifyAttributes(roleDn, mods);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeLDAPRoles(DirContext ctx, String email, String role,
			DirContext rootContext) {
		String roleDn = getGroupDn(ctx, role);
		System.out.println("roleDn = " + roleDn);

		ModificationItem[] mods = new ModificationItem[1];
		String userDn = getUserDn(ctx, email);
		System.out.println("userDn = " + userDn);
		Attribute mod = new BasicAttribute("uniqueMember", userDn);
		mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, mod);
		try {
			System.out.println("Inside Try Role DN----" + roleDn);
			System.out.println("Inside Try mods----" + mods);
			rootContext.modifyAttributes(roleDn, mods);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getUserDn(DirContext ctx, String email) {
		String dn = null;
		String[] returnAttributes = { "distinguishedName" };
		SearchControls searchCtls = new SearchControls();
		String filter = "(mail=" + email + ")";
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchCtls.setReturningAttributes(returnAttributes);
		NamingEnumeration<SearchResult> results;
		try {
			results = ctx.search("", filter, searchCtls);
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();

				System.out.println("distinguishedName = "
						+ searchResult.getNameInNamespace());
				dn = searchResult.getNameInNamespace();

				dn = ("" + dn).replace("\\", "\\");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dn;
	}

	private String getGroupDn(DirContext ctx, String groupName) {
		String[] returnAttributes = { "distinguishedName" };
		System.out.println("distinguishedName");
		String dn = null;
		SearchControls searchCtls = new SearchControls();
		String filter = "(cn=" + groupName + ")";
		System.out.println("filter--->" + filter);
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchCtls.setReturningAttributes(returnAttributes);
		NamingEnumeration<SearchResult> results;
		try {
			results = ctx.search("", filter, searchCtls);
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();

				System.out.println("distinguishedName = "
						+ searchResult.getNameInNamespace());
				dn = searchResult.getNameInNamespace();

				dn = ("" + dn).replace("\\", "\\");

				System.out.println("-----dn-----" + dn);
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dn;
	}

	public List getLDAPGroupMembership(DirContext ctx,
			String returningAttribute, String attributeName,
			String attributeValue) throws Exception {
		List<String> userGroups = new ArrayList<String>();
		SearchControls sc = new SearchControls();
		String[] attributeFilter = { returningAttribute };
		sc.setReturningAttributes(attributeFilter);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String filter = "(" + attributeName + "=" + attributeValue + ")";
		System.out.println("getLDAPGroupMembership :: search filter " + filter);
		NamingEnumeration results = ctx.search(searchBase, filter, sc);
		while (results.hasMore()) {
			SearchResult sr = (SearchResult) results.next();
			Attributes attrs = sr.getAttributes();
			if (null != attrs) {
				for (NamingEnumeration ae = attrs.getAll(); ae
						.hasMoreElements();) {
					Attribute atr = (Attribute) ae.next();
					String attributeID = atr.getID();
					Enumeration vals = atr.getAll();

					while (vals.hasMoreElements()) {
						String username = (String) vals.nextElement();
						String[] str = username.split(",");
						String[] str1 = str[0].split("=");
						System.out.println("Groups values are as------->"
								+ str1[1]);
						if (null != str1[1])
							userGroups.add(str1[1]);
					}
				}
			} else {
				System.out.println("Fetching attribute having no value");
			}

		}
		return userGroups;
	}

	public List getLDAPGroupsbyUserType(DirContext ctx,
			String returningAttribute, String attributeName,
			String attributeValue, String businessUnit) throws Exception {

		String searchQuery = null;

		if (null != businessUnit && businessUnit.equals("ICC")) {
			//searchQuery = "(&(objectClass=groupofUniqueNames)(cn=*ic-c*))";
			searchQuery = "(&(objectClass=groupofUniqueNames)(|(cn=*ic-c*)(cn=*SPOTFIRE-EXT*)))";
		} else {
			searchQuery = "(&(objectClass=groupofUniqueNames)(|(cn=*investigatorportal*)(cn=*ic-s*)))";
		}

		System.out.println("getLDAPGroupsbyUserType:: searchQuery:: "
				+ searchQuery);

		return sarchLDAPGroups(ctx, returningAttribute, searchQuery);
	}

	public List sarchLDAPGroups(DirContext ctx, String returningAttribute,
			String searchQuery) throws Exception {

		List<String> matchingResults = new ArrayList<String>();

		SearchControls searchcontrols = new SearchControls();
		searchcontrols
				.setReturningAttributes(new String[] { returningAttribute });
		searchcontrols.setSearchScope(SearchControls.SUBTREE_SCOPE);

		NamingEnumeration namingEnumeration = ctx.search(ldapParam,
				searchQuery, searchcontrols);

		while (namingEnumeration.hasMoreElements()) {
			SearchResult searchresult = (SearchResult) namingEnumeration
					.nextElement();
			if (searchresult != null) {
				Attributes attrs = searchresult.getAttributes();
				if (attrs != null && attrs.size() != 0) {
					Attribute resultAttrValue = attrs.get(returningAttribute);
					if (resultAttrValue != null) {
						String temp = (String) resultAttrValue.get();
						matchingResults.add(temp);
						System.out.println("Attribute value attributeValue="
								+ temp);
					}
				}

			}
		}
		return matchingResults;
	}

	public String getBusinessUnitfromAD(DirContext ctx, String adRootContext,
			String email) throws Exception {
		List<String> matchingResults = new ArrayList<String>();

		SearchControls searchcontrols = new SearchControls();
		searchcontrols
				.setReturningAttributes(new String[] { "businessCategory" });
		searchcontrols.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String businessUnit = "";
		String searchQuery = "(&(objectClass=*)(mail=" + email + "))";
		NamingEnumeration namingEnumeration = ctx.search(adRootContext,
				searchQuery, searchcontrols);

		while (namingEnumeration.hasMoreElements()) {
			SearchResult searchresult = (SearchResult) namingEnumeration
					.nextElement();
			if (searchresult != null) {
				Attributes attrs = searchresult.getAttributes();
				if (attrs != null && attrs.size() != 0) {
					Attribute resultAttrValue = attrs.get("businessCategory");
					if (resultAttrValue != null) {
						businessUnit = (String) resultAttrValue.get();
						System.out.println("Attribute value attributeValue="
								+ businessUnit);
					}
				}
			}
		}
		return businessUnit;
	}

	public Map<String, String> getADUserData(String email, DirContext ctx)
			throws NamingException {
		Map<String, String> userDataAD = new HashMap<String, String>();
		String searchFilter = null;
		SearchControls sc = new SearchControls();
		String[] attributeFilter = { "mail", "samAccountName",
				"distinguishedName", "userAccountControl", "sn", "givenName",
				"employeeID", "businessCategory", "description", "whenCreated",
				"company" };
		sc.setReturningAttributes(attributeFilter);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

		searchFilter = "(|(&(objectClass=user)(samAccountName=" + email
				+ "))(&(objectClass=user)(mail=" + email + ")))";

		NamingEnumeration results = null;
		results = ctx.search(adBase, searchFilter, sc);
		System.out.println("LDAPUtils:: getADUserData <<<<");

		while (results.hasMore()) {
			SearchResult sr = (SearchResult) results.next();
			Attributes attrs = sr.getAttributes();
			Attribute attr = null;
			try {
				if (null != attrs) {
					attr = attrs.get("mail");

					if (null != attr) {
						userDataAD.put("mail", attr.get().toString());
					} else {
						userDataAD.put("mail", "");
					}
					attr = attrs.get("samAccountName");
					if (null != attr) {
						userDataAD.put("samAccountName", attr.get().toString());
					} else {
						userDataAD.put("samAccountName", "");
					}
					attr = attrs.get("distinguishedName");
					if (null != attr) {
						userDataAD.put("distinguishedName", attr.get()
								.toString());
					} else {
						userDataAD.put("distinguishedName", "");
					}
					attr = attrs.get("userAccountControl");
					if (null != attr) {
						String Status = attr.get().toString();
						String state = "514";
						if (Status.equalsIgnoreCase(state)) {
							userDataAD.put("Status", "DISABLED");
						} else
							userDataAD.put("Status", "ACTIVE");
					} else {
						userDataAD.put("Status", "");
					}
					attr = attrs.get("sn");
					if (null != attr) {
						userDataAD.put("sn", attr.get().toString());
					} else {
						userDataAD.put("sn", "");
					}

					attr = attrs.get("givenName");
					if (null != attr) {
						userDataAD.put("givenName", attr.get().toString());
					} else {
						userDataAD.put("givenName", "");
					}

					attr = attrs.get("employeeID");
					if (null != attr) {
						userDataAD.put("employeeID", attr.get().toString());
					} else {
						userDataAD.put("employeeID", "");
					}

					attr = attrs.get("businessCategory");
					if (null != attr) {
						if (attr.get().toString().equalsIgnoreCase("ICC")) {
							userDataAD.put("businessCategory", "Customer");
						} else {
							userDataAD.put("businessCategory", "Investigator");
						}

					} else {
						userDataAD.put("businessCategory", "");
					}

					attr = attrs.get("company");
					if (null != attr) {
						userDataAD.put("company", attr.get().toString());
					} else {
						userDataAD.put("company", "");
					}

					attr = attrs.get("whenCreated");
					if (null != attr) {
						String date = attr.get().toString();
						String date2 = date.substring(0, date.indexOf('.'));
						System.out.println("date2 in AD:: " + date2);
						DateFormat formatter = new SimpleDateFormat(
								"yyyyMMddHHmmss");
						Date date1 = (Date) formatter.parse(date2);
						System.out.println("date1 in AD " + date1);
						SimpleDateFormat newFormat = new SimpleDateFormat(
								"dd-MMM-yyyy HH:mm:ss");
						String finalStringDate = newFormat.format(date1);
						System.out.println("finalStringDate IN AD::"
								+ finalStringDate);
						userDataAD.put("whenCreated", finalStringDate);
					} else {
						userDataAD.put("whenCreated", "");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return userDataAD;
	}

	public Map<String, String> getOUDUserData(String email, DirContext ctx)
			throws NamingException {
		Map<String, String> userDataOUD = new HashMap<String, String>();
		String searchFilter = null;
		// String base = "OU=INCRDC_USERS";
		SearchControls sc = new SearchControls();
		String[] attributeFilter = { "mail", "orclSAMAccountName",
				"roomNumber", "givenname", "sn", "orclIsEnabled",
				"employeenumber", "departmentnumber", "createTimestamp" };
		sc.setReturningAttributes(attributeFilter);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

		searchFilter = "(|(&(objectClass=inetOrgPerson)(orclSAMAccountName="
				+ email + "))(&(objectClass=inetOrgPerson)(mail=" + email
				+ ")))";

		NamingEnumeration results = null;
		results = ctx.search("", searchFilter, sc);
		while (results.hasMore()) {
			SearchResult sr = (SearchResult) results.next();
			Attributes attrs = sr.getAttributes();
			Attribute attr = null;
			try {
				if (null != attrs) {
					attr = attrs.get("mail");
					if (null != attr) {
						userDataOUD.put("mail", attr.get().toString());
					} else {
						userDataOUD.put("mail", "");
					}
					attr = attrs.get("orclSAMAccountName");
					if ((null != attr && null != attr.get() && !("" + attr
							.get()).trim().equals(""))) {
						userDataOUD.put("orclSAMAccountName", attr.get()
								.toString());
						System.out
								.println("value orclSAMAccountName is coming as-->"
										+ attr.get().toString());
					} else {
						userDataOUD.put("orclSAMAccountName", "");
					}
					attr = attrs.get("roomNumber");
					if (null != attr) {
						userDataOUD.put("roomNumber", attr.get().toString());
						System.out.println("value roomNumber is coming as-->"
								+ attr.get().toString());
					} else {
						userDataOUD.put("roomNumber", "");
					}

					attr = attrs.get("givenname");
					if (null != attr) {
						userDataOUD.put("givenname", attr.get().toString());
						System.out.println("value givenname is coming as-->"
								+ attr.get().toString());
					} else {
						userDataOUD.put("givenname", "");
					}

					attr = attrs.get("sn");
					if (null != attr) {
						userDataOUD.put("sn", attr.get().toString());
						System.out.println("value sn is coming as-->"
								+ attr.get().toString());
					} else {
						userDataOUD.put("sn", "");
					}

					attr = attrs.get("departmentnumber");
					if (null != attr) {
						userDataOUD.put("departmentnumber", attr.get()
								.toString());
						System.out
								.println("value departmentnumber is coming as-->"
										+ attr.get().toString());
					} else {
						userDataOUD.put("departmentnumber", "");
					}

					attr = attrs.get("orclIsEnabled");
					if (null != attr) {
						userDataOUD.put("orclIsEnabled", attr.get().toString());
						System.out
								.println("value orclIsEnabled is coming as-->"
										+ attr.get().toString());
					} else {
						userDataOUD.put("orclIsEnabled", "");
					}

					attr = attrs.get("employeenumber");
					if (null != attr) {
						userDataOUD
								.put("employeenumber", attr.get().toString());
						System.out
								.println("value employeenumber is coming as-->"
										+ attr.get().toString());
					} else {
						userDataOUD.put("employeenumber", "");
					}

					attr = attrs.get("createTimestamp");
					if (null != attr) {
						String date = attr.get().toString();
						System.out.println("Date in OUD is coming As::" + date);
						String date2 = date.substring(0, 14);
						System.out.println("date2 in OUD:: " + date2);
						DateFormat formatter = new SimpleDateFormat(
								"yyyyMMddHHmmss");
						Date date1 = (Date) formatter.parse(date2);
						System.out.println("date1 in OUD:: " + date1);
						SimpleDateFormat newFormat = new SimpleDateFormat(
								"dd-MMM-yyyy HH:mm:ss");
						String finalStringDate = newFormat.format(date1);
						System.out.println("finalStringDate IN OUD::"
								+ finalStringDate);
						userDataOUD.put("createTimestamp", finalStringDate);

					} else {
						userDataOUD.put("createTimestamp", "");
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return userDataOUD;
	}

	public String getUserDnAD(DirContext ctx, String email) {
		String dn = "N.A.";
		String[] returnAttributes = { "distinguishedName" };
		SearchControls searchCtls = new SearchControls();
		String filter = "(mail=" + email + ")";
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchCtls.setReturningAttributes(returnAttributes);
		NamingEnumeration<SearchResult> results;
		try {
			results = ctx.search(rootContext, filter, searchCtls);
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();

				System.out.println("distinguishedName = "
						+ searchResult.getNameInNamespace());
				dn = searchResult.getNameInNamespace();
				if (null != dn || !(dn.isEmpty())) {
					dn = ("" + dn).replace("\\", "\\");
				} else {
					dn = "N.A.";
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dn;
	}
}
