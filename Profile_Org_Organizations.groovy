package com.openjaw.tdata.pseudonymise

globalMap.put("ReplaceProfileOrgOrganizationsData", new ReplaceProfileOrgOrganizationsData())

/**
 * This class is used to replace or encrypt the identifying data in a SQL resultset row
 *
 */
class ReplaceProfileOrgOrganizationsData {
	private static final String NAME_PREFIX = "Organization number "
	private static final String ADDRESS_LINE = "addr 1"
	private static final String ADDRESS_LINE_B = "addr b1"
	private static final String POST_CODE = "0001"
	private static final String POST_CODE_B = "0001B"
	private static final String EMAIL = "john.smith@colorline.com"
	private static final String AMADEUS_ID_PREFIX = "0000"

	/**
     *  Replaces or encrypts identifying data in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void processIdentifyingData(row) {
		if(null != row){
			replaceIdentifyingData(row)
		}
	}

	/**
     *  Replaces identifying data contained in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void replaceIdentifyingData(row) {
		println("Starting a row : " + row.ORGANIZATION_ID)

		row.ORGANIZATION_NAME = NAME_PREFIX + row.ORGANIZATION_ID
		if (row.CODE != null) row.CODE = row.ORGANIZATION_ID
		row.ADDRESS_LINE = ADDRESS_LINE ;
		row.ADDRESS_LINE_B = ADDRESS_LINE_B ;
		row.POSTAL_CODE = POST_CODE ;
		row.POSTAL_CODE_B = POST_CODE_B ;
		if (row.AMADEUS_ID != null) row.AMADEUS_ID = AMADEUS_ID_PREFIX + row.ORGANIZATION_ID
		row.COMPANY_EMAIL = EMAIL ;

		println("Finished a row : " + row.ORGANIZATION_ID)
	}



	/*
	 * utility methods
	 */

	static convertToAlpha(int value){
		String result = ""
		char start_char = 'A'

		while(value != 0){
			result += (char)(start_char + (value % 10))
			value /= 10
		}

		return result
	}

	static convertToAlpha(String value){
		if(value?.trim()){
			String result = ""
			char[] characters = value.toCharArray()
			char start_char = 'A'

			for(ch in characters){
				if(Character.isDigit(ch)){
					result += (char)(start_char + Character.getNumericValue(ch))
				}
				else if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')){
					result += ch
				}
				else{
					result += "Z"
				}
			}

			return result
		}

		return value
	}
}
