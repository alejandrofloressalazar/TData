package com.openjaw.tdata.pseudonymise

globalMap.put("ReplaceProfileOrgContactPersonsData", new ReplaceProfileOrgContactPersonsData())

/**
 * This class is used to replace or encrypt the identifying data in a SQL resultset row
 *
 */
class ReplaceProfileOrgContactPersonsData {
	private static final String NAME_PREFIX = "Ms."
	private static final String FIRST_NAME = "Anne-Lise"
	private static final String LAST_NAME = "Contact"
	private static final String ADDRESS_LINE = "addr 1"
	private static final String POST_CODE = "0001"
	private static final String EMAIL = "annelise.contact@colorline.com"
	private static final String EMPLOYEE_TITLE = "Employee title"

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
		println("Starting a row : " + row.CONTACT_PERSON_ID)

		row.NAME_PREFIX = NAME_PREFIX 
		row.FIRSTNAME = FIRST_NAME
		row.LASTNAME = LAST_NAME
		row.ADDRESS_LINE = ADDRESS_LINE ;
		row.POSTAL_CODE = POST_CODE ;
		row.EMAIL = EMAIL ;
		row.EMPLOYEE_TITLE = EMPLOYEE_TITLE

		println("Finished a row : " + row.CONTACT_PERSON_ID)
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
