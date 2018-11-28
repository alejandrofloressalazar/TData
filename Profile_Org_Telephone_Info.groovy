package com.openjaw.tdata.pseudonymise

globalMap.put("ReplaceProfileOrgTelephoneInfoData", new ReplaceProfileOrgTelephoneInfoData())

/**
 * This class is used to replace or encrypt the identifying data in a SQL resultset row
 *
 */
class ReplaceProfileOrgTelephoneInfoData {
	private static final String NUMBER_PREFIX = "00"

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
		println("Starting a row : " + row.TELEPHONE_INFO_ID)

		row.PHONE_NUMBER = NUMBER_PREFIX + row.TELEPHONE_INFO_ID

		println("Finished a row : " + row.TELEPHONE_INFO_ID)
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
