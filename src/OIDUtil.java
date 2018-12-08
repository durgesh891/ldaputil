
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import java.io.FileInputStream;

public class OIDUtil<E> {

	// required private variables
	private Properties properties;
	private static DirContext adContext;
	private static DirContext oidContext;

	private SearchControls searchCtls;
	private String baseFilter = "(&((&(objectCategory=Person)(objectClass=User)))";
	private static Properties configs;
	private static int totalOIDRecords = 0;
	private static int totalADRecords = 0;
	private static int matchedOIDAccounts = 0;
	private static List<String> EBSStats = new ArrayList<String>();
	private static List<String> ArgusStats = new ArrayList<String>();

	static {
		try {
			String propFileName = System.getProperty("user.dir")+"/config.properties";
			configs = new Properties();
			configs.load(new FileInputStream(propFileName));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * method with parameter for initializing a LDAP context
	 * 
	 * @param username
	 *            a {@link java.lang.String} object - username to establish a
	 *            LDAP connection
	 * @param password
	 *            a {@link java.lang.String} object - password to establish a
	 *            LDAP connection
	 * @param domainController
	 *            a {@link java.lang.String} object - domain controller name for
	 *            LDAP connection
	 */
	private DirContext getDirectoryContext(String username, String password,
			String host, String port) throws NamingException {

		properties = new Properties();
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

	/**
	 * search the Active directory by username/email id for given search base
	 * 
	 * @param searchValue
	 *            a {@link java.lang.String} object - search value used for AD
	 *            search for eg. username or email
	 * @param searchBy
	 *            a {@link java.lang.String} object - scope of search by
	 *            username or by email id
	 * @param searchBase
	 *            a {@link java.lang.String} object - search base value for
	 *            scope tree for eg. DC=myjeeva,DC=com
	 * @return search result a {@link javax.naming.NamingEnumeration} object -
	 *         active directory search result
	 * @throws NamingException
	 */
	// @LDAPUTILUSED String filter = "(&(objectClass=person))";
	public NamingEnumeration<SearchResult> searchOIDUsers(String filter,DirContext oidContext )
			throws NamingException {
		// String base = getDomainBase(searchBase);
		String base = configs.getProperty("oud.domainbase");
		// String filter = getFilter(searchValue, searchBy);
		//String filter = "(objectclass=person)";
		
		// initializing search controls
		searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String[] returnAttributes = { "orclnormdn", "uid", "employeeNumber" };
		searchCtls.setReturningAttributes(returnAttributes);
		return oidContext.search(base, filter, this.searchCtls);
	}

	/*
	 * public NamingEnumeration<SearchResult> searchADUsers() throws
	 * NamingException { // String base = getDomainBase(searchBase); String base
	 * = configs.getProperty("ad.domainbase"); // String filter =
	 * getFilter(searchValue, searchBy); String filter = "(objectClass=user)";
	 * // initializing search controls searchCtls = new SearchControls();
	 * searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE); String[]
	 * returnAttributes = { "employeeID", "DistinguishedName" };
	 * searchCtls.setReturningAttributes(returnAttributes); return
	 * this.adContext.search(base, filter, this.searchCtls); }
	 */

	/**
	 * closes the LDAP connection with Domain controller
	 */
	public void closeLdapConnection() {
		try {
			if (adContext != null)
				adContext.close();
		} catch (NamingException e) {
			log("", e.getMessage(), e);
		}
		try {
			if (oidContext != null)
				oidContext.close();
		} catch (NamingException e) {
			log("", e.getMessage(), e);
		}
	}

	/**
	 * active directory filter string value
	 * 
	 * @param searchValue
	 *            a {@link java.lang.String} object - search value of
	 *            username/email id for active directory
	 * @param searchBy
	 *            a {@link java.lang.String} object - scope of search by
	 *            username or email id
	 * @return a {@link java.lang.String} object - filter string
	 */
	private String getFilter(String searchValue, String searchBy) {
		String filter = this.baseFilter;
		if (searchBy.equals("uid")) {
			filter += "(uid=" + searchValue + "))";
		} else if (searchBy.equals("username")) {
			filter += "(samaccountname=" + searchValue + "))";
		}
		return filter;
	}

	private String getOrclMTUid(String uid) {
		String orclMTUid = uid;
		if (uid != null && uid.indexOf("@") != -1) {
			orclMTUid = uid.substring(0, uid.indexOf("@"));

		}
		return orclMTUid;
	}

	private void modifyAttrValue(String attrToReplace, String attrValue,
			String dn) {
		ModificationItem[] item = new ModificationItem[1];
		Attribute attribute = new BasicAttribute(attrToReplace, attrValue);
		item[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute);
		try {
			oidContext.modifyAttributes(dn, item);
			System.out.println("Attribute modified----------> " + dn);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	// TODO replace SOP with actual logger statement
	private void log(String level, String message, Throwable t) {
		System.out.println("OIDUtil:" + message);
	}

	private void populateOrclMTUid() {
		String orclMTUid = null;
		try {
			NamingEnumeration<SearchResult> results = searchOIDUsers(baseFilter,oidContext);
			String dn = null;
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();
				Attributes attributes = searchResult.getAttributes();

				Attribute attr = attributes.get("orclnormdn");
				if (null != attr) {
					dn = ("" + attr.get()).replace("\\", "\\");
					// System.out.println("dn----------> " + dn);
				}
				attr = attributes.get("uid");
				if (null != attr) {
					orclMTUid = getOrclMTUid("" + attr.get());
					addOIDObjClass("orclIDXPerson", dn);
					ArgusStats.add(dn);
					System.out.println("Going to modify attribute value" + dn);
					modifyAttrValue("orclMTUid", orclMTUid.toLowerCase(), dn);
				}
				attr = attributes.get("orclMTUid");
				if (null != attr) {
					// System.out
					// .println("old orclMTUid----------> " + attr.get());
				}
				totalOIDRecords++;
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void populateOrclSourceObjectDN(Map<String, String> oidMap) {

		try {
			NamingEnumeration<SearchResult> results = null;
			// Activate paged results
			int pageSize = 50;
			byte[] cookie = null;
			int total;
			// String
			// base="ou=EMPLOYEES,ou=USERS,ou=Sites,dc=corp-oid,dc=dev-incresearch,dc=com";
			String base = configs.getProperty("ad.domainbase");
			//String filter = "(objectClass=user)";
			String filter = "(objectClass=user)";

			System.out.println("AD base for search is--->" + base);

			((InitialLdapContext) adContext)
					.setRequestControls(new Control[] { new PagedResultsControl(
							pageSize, Control.NONCRITICAL) });

			searchCtls = new SearchControls();
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] returnAttributes = { "employeeID", "DistinguishedName" };
			searchCtls.setReturningAttributes(returnAttributes);
			do {
				/* perform the search */
				try {
					results = adContext.search(base, filter, searchCtls);
				} catch (Exception e) {

					System.out.println(e.getMessage());
				}

				// NamingEnumeration<SearchResult> results =
				// searchADUsers(filter);
				String dn = null;
				String employeenumber = null;
				while (results.hasMore()) {
					SearchResult searchResult = (SearchResult) results.next();
					Attributes attributes = searchResult.getAttributes();

					Attribute attr = attributes.get("employeeID");
					if (null != attr) {
						employeenumber = "" + attr.get();
						// System.out.println("employeeID----------> "
						// + employeenumber);
					}
					attr = attributes.get("DistinguishedName");
					if (null != attr) {
						dn = "" + attr.get();

						System.out.println("AD DistinguishedName----------> "
								+ dn);
					}
					totalADRecords++;

					// System.out.println("count----------> " + count);
					if (null != oidMap.get(employeenumber)) {
						matchedOIDAccounts++;
						// System.out.println("oidMap.get(employeenumber)---------->"
						// + oidMap.get(employeenumber));

						System.out
								.println("oidMap.get(employeenumber)---------->"
										+ oidMap.get(employeenumber));

						addOIDObjClass("orclADObject",
								"" + oidMap.get(employeenumber));

						modifyAttrValue("orclSourceObjectDN", dn,
								"" + oidMap.get(employeenumber));
						EBSStats.add(dn);

					}

				}
				// Examine the paged results control response
				Control[] controls = ((InitialLdapContext) adContext)
						.getResponseControls();
				if (controls != null) {
					for (int i = 0; i < controls.length; i++) {
						if (controls[i] instanceof PagedResultsResponseControl) {
							PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
							total = prrc.getResultSize();
							if (total != 0) {
								System.out.println("Scheduler Test  "
										+ "***************** END-OF-PAGE"
										+ "(total : " + total
										+ ") *****************\n");

							} else {
								// System.out.println("***************** END-OF-PAGE "
								// +
								// "(total: unknown) ***************\n");
								System.out.println("Scheduler Test  "
										+ "***************** END-OF-PAGE"
										+ "(total: unknown) ***************\n");
							}
							cookie = prrc.getCookie();
						}
					}
				} else {
					System.out.println("Scheduler Test  "
							+ "No controls were sent from the server");

				}
				// Re-activate paged results
				((InitialLdapContext) adContext)
						.setRequestControls(new Control[] { new PagedResultsControl(
								pageSize, cookie, Control.CRITICAL) });
			} while (cookie != null);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException ie) {
			System.err.println("PagedSearch failed.");

		}
	}

	private void addOIDObjClass(String objClass, String dn) {
		ModificationItem[] classModification = new ModificationItem[1];
		classModification[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE,
				new BasicAttribute("objectclass", objClass));

		try {
			oidContext.modifyAttributes(dn, classModification);
		} catch (NamingException e1) {
			// TODO Auto-generated catch block
			System.out.println("Object class " + objClass + "already present");
		}
	}

	private Map<String, String> buildOIDMap() {
		Map<String, String> oidMap = new HashMap<String, String>();
		try {
			NamingEnumeration<SearchResult> results = searchOIDUsers(baseFilter,oidContext);
			String dn = null;
			String employeeNumber = null;
			while (results.hasMore()) {
				SearchResult searchResult = (SearchResult) results.next();
				Attributes attributes = searchResult.getAttributes();

				Attribute attr = attributes.get("orclnormdn");
				if (null != attr) {
					dn = ("" + attr.get()).replace("\\", "\\");
					// System.out.println("dn----------> " + dn);
				}
				attr = attributes.get("employeeNumber");
				if (null != attr) {
					employeeNumber = "" + attr.get();
					oidMap.put(employeeNumber, dn);
				}
				totalOIDRecords++;
				// System.out.println("count----------> " + count);

			}

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oidMap;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String utilMode = configs.getProperty("utility.data.source");
		OIDUtil oidUtil = new OIDUtil();
		System.out.println("OIDUtil:" + "Making OID connction");
		System.out.println("OIDUtil:" + configs.getProperty("oid.username")
				+ ":::" + configs.getProperty("oid.password") + ":::"
				+ configs.getProperty("oid.host") + ":::"
				+ configs.getProperty("oid.port"));

		try {
			oidContext = oidUtil.getDirectoryContext(
					configs.getProperty("oid.username"),
					configs.getProperty("oid.password"),
					configs.getProperty("oid.host"),
					configs.getProperty("oid.port"));
			System.out.println("OIDUtil:" + "OID Connction established");

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ("argus".equalsIgnoreCase(utilMode)) {
			System.out.println("Running utility in Argus Mode");
			oidUtil.populateOrclMTUid();

		} else if ("ebs".equalsIgnoreCase(utilMode)) {
			Map<String, String> oidMap = oidUtil.buildOIDMap();
			System.out.println("OIDUtil:" + "Making AD connction");
			System.out.println("OIDUtil:" + configs.getProperty("ad.username")
					+ ":::" + configs.getProperty("ad.password") + ":::"
					+ configs.getProperty("ad.host") + ":::"
					+ configs.getProperty("ad.port"));

			try {
				adContext = oidUtil.getDirectoryContext(
						configs.getProperty("ad.username"),
						configs.getProperty("ad.password"),
						configs.getProperty("ad.host"),
						configs.getProperty("ad.port"));
				System.out.println("OIDUtil:" + "AD Connction established");
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			oidUtil.populateOrclSourceObjectDN(oidMap);

		}
		oidUtil.closeLdapConnection();
		System.out.println("OIDUtil:" + "Connctions closed");

		System.out.println("*********** Printing Final Stats ***************");
		System.out.println("************************************************");
		System.out.println("Total OID Accounts read: " + totalOIDRecords);
		if ("ebs".equalsIgnoreCase(utilMode)) {
			System.out.println("Total AD accounts read: " + totalADRecords);
			System.out.println("No. of AD accounts matched wih OID accounts: "
					+ matchedOIDAccounts);
			System.out
					.println("No. of OID accounts that found corresponding AD accounts: "
							+ matchedOIDAccounts);
			System.out
					.println("No. of OID accounts that did not find corresponding AD Accounts: "
							+ (totalOIDRecords - matchedOIDAccounts));
			System.out.println("List of updated OID accounts, printed below:");
			if (EBSStats.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < EBSStats.size(); i++) {
					sb.append(EBSStats.get(i) + ", ");
				}
				System.out.println(sb.toString());
			} else {
				System.out.println("None");
			}
		} else {
			System.out.println("List of updated OID accounts, printed below:");
			if (ArgusStats.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < ArgusStats.size(); i++) {
					sb.append(ArgusStats.get(i) + ", ");
				}
				System.out.println(sb.toString());
			} else {
				System.out.println("None");
			}

		}
		System.out.println("************************************************");
		System.out.println("************************************************");
	}

}
