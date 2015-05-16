package de.hahnjo.android.smbprovider.provider;

/**
 * A class containing utility-methods for getting information from the documentId.
 */
public class DocumentIdUtils {

	/**
	 * @return the account name of this documentId
	 */
	public static String getAccountName(String documentId) {
		return documentId.substring(0, documentId.indexOf('/'));
	}

	/**
	 * @return the path of this documentId
	 */
	public static String getPath(String documentId) {
		return documentId.substring(documentId.indexOf('/'));
	}

	/**
	 * @return the name of this documentId
	 */
	public static String getName(String documentId) {
		int nameEnd;
		if (isDirectory(documentId)) {
			nameEnd = documentId.length() - 1;
		} else {
			nameEnd = documentId.length();
		}
		int nameStart = documentId.lastIndexOf('/', nameEnd - 1) + 1;
		return documentId.substring(nameStart, nameEnd);
	}

	/**
	 * @return true, if this documentId points to a directory
	 */
	public static boolean isDirectory(String documentId) {
		return documentId.endsWith("/");
	}

	/**
	 * @return true, if this documentId points to a root
	 */
	public static boolean isRoot(String documentId) {
		return documentId.indexOf('/') == (documentId.length() - 1);
	}

}
