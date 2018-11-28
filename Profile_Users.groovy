package com.openjaw.tdata.pseudonymise

import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

globalMap.put("ReplaceProfileUsersPersonalData", new ReplaceProfileUsersPersonalData())

/**
 * This class is used to replace or encrypt the personal data in a SQL resultset row
 *
 * Possible issues: DOB and PASSWD
 */
class ReplaceProfileUsersPersonalData {
	private static final String NAME_PREFIX = "Mr."
	private static final String GIVEN_NAME = "John"
	private static final String MIDDLE_NAME = "Paul"
	private static final String SURNAME = "Smith"
	private static final String GIVEN_NAME_SIMPLIFIED = "John"
	private static final String MIDDLE_NAME_SIMPLIFIED = "Paul"
	private static final String SURNAME_SIMPLIFIED = "Smith"
	private static final String FULL_NAME = "John Paul Smith"
	private static final String ADDRESS_LINE = "addr"
	private static final String POST_CODE = "0001"
	private static final String TELEPHONE = "4720000000"
	private static final String EMAIL = "john.smith@colorline.com"
	private static final String EMAIL_DOMAIN = "@colorline.com"
	private static final String LOYALTY = "11111111"
	private static final String DOCUMENT = "1111111111"
	private static final String AGENT_ID = "agent1"
	private static final String IATA_NUMBER = "0000"
	private static final String EMAIL_HASH = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
	private static final String PASSWORD_HASH = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
	private static final String PASSWORD = '{AES}5FdfPukwZBx+G0rYE1wynw=='
	private static final String REMARK_TEXT = "this is a remark"
	private static final String COMMENT_TEXT = "this is a comment"

	/**
     *  Replaces or encrypts personal data in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void processPersonalData(row) {
		if(null != row){
			replacePersonalData(row)
		}
	}

	/**
     *  Replaces personal data contained in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void replacePersonalData(row) {
		println("Starting a row " + row.CUSTOMER_ID)
		String userProfileId = row.CUSTOMER_ID
		String newEmail, newName

		if(userProfileId?.trim()) {
			newName = convertToAlpha(userProfileId)
			row.CUSTOMER_NAME = newName + " " + SURNAME
			newEmail = newName + "." + SURNAME + EMAIL_DOMAIN
		}
		else {
			newName = GIVEN_NAME + "A"
			row.CUSTOMER_NAME = newName + " " + SURNAME
			newEmail = newName + "." + SURNAME + EMAIL_DOMAIN
		}
		row.EMAIL = newEmail 
		row.PHONE_NUMBER = TELEPHONE
		row.LAST_MODIFIED_BY = newModifiedBy(row.LAST_MODIFIED_BY)

		if(row.USERDATA) { row.USERDATA = replaceProfilePersonalData(row.USERDATA, newEmail, newName) }
		if (row.REMARKS) { row.REMARKS = processRemarksPersonalData(row.REMARKS) }
		if (row.COMMENTS) { row.COMMENTS = processRemarksPersonalData(row.COMMENTS) }
		row.PASSWD = PASSWORD 
		if (row.DOB) row.DOB = newDateOfBirth(row.DOB)
		println("Finished a row " + row.CUSTOMER_ID)
	}

	static String replaceProfilePersonalData(String xml, String newEmail, String newName) {
		if(null == xml || "".equals(xml.trim())) {
			return xml
		}

		def Profile = new XmlSlurper().parseText(xml)

		if (!Profile.Customer.@BirthDate.isEmpty()) {
			String oldValue = Profile.Customer.@BirthDate.text()
			Profile.Customer.@BirthDate = oldValue.substring(0, 5) + "01-01"
		} 

		Profile.Customer.PersonName.NamePrefix = NAME_PREFIX
		Profile.Customer.PersonName.GivenName = newName
		Profile.Customer.PersonName.MiddleName = MIDDLE_NAME
		Profile.Customer.PersonName.Surname = SURNAME
		Profile.Customer.PersonName.GivenName.@Simplified = newName
		Profile.Customer.PersonName.MiddleName.@Simplified = MIDDLE_NAME_SIMPLIFIED
		Profile.Customer.PersonName.Surname.@Simplified = SURNAME_SIMPLIFIED
		Profile.Customer.Telephone.@PhoneNumber = TELEPHONE
		Profile.Customer.Address.AddressLine = ADDRESS_LINE
		Profile.Customer.Address.PostalCode = POST_CODE
		Profile.Customer.Email = newEmail

		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.CardHolderName = FULL_NAME
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.NamePrefix = NAME_PREFIX
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.GivenName = GIVEN_NAME
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.MiddleName = MIDDLE_NAME
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.Surname = SURNAME
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.GivenName.@Simplified = GIVEN_NAME_SIMPLIFIED
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.MiddleName.@Simplified = MIDDLE_NAME_SIMPLIFIED
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.PersonName.Surname.@Simplified = SURNAME_SIMPLIFIED
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.Address.AddressLine = ADDRESS_LINE
		Profile.PaymentDetails.Payments.Payment.PaymentForm.'*'.Address.PostalCode = POST_CODE

		Profile.PaymentDetails.Payments.Payment.Agent.@Agent = AGENT_ID
		Profile.PaymentDetails.Payments.Payment.Agent.@IATANumber = IATA_NUMBER

		Profile.TPA_Extensions.PasswordHash = PASSWORD_HASH
		Profile.TPA_Extensions.EmailHash = EMAIL_HASH

		String serializedProfile  = XmlUtil.serialize(Profile)
		//println("Serialized profile: " + serializedProfile)

		serializedProfile 
	}

	static String newModifiedBy(String oldValue) {
		if ('IBE' == oldValue) {  return oldValue }
		else if ('admin' == oldValue) { return oldValue }
		AGENT_ID
	}

	static java.util.Date newDateOfBirth(oldValue) {
		java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy")
		Calendar cal = Calendar.getInstance()
		cal.set(Integer.parseInt(df.format(oldValue),10), 00, 01)
		new java.util.Date(cal.getTimeInMillis())
	}

	static String processRemarksPersonalData(String xml) {
		if(null == xml || "".equals(xml.trim())) {
			return xml
		}

		def Remarks = new XmlSlurper().parseText(xml)

		Remarks.Remark.Text = REMARK_TEXT

		Remarks.Remark.Agent.@Agent = AGENT_ID
		Remarks.Remark.Agent.@IATANumber = IATA_NUMBER

		return XmlUtil.serialize(Remarks)
	}

	static String processCommentsPersonalData(String xml) {
		if(null == xml || "".equals(xml.trim())) {
			return xml
		}

		def Remarks = new XmlSlurper().parseText(xml)

		if(!Remarks.Text.isEmpty()) {
			Remarks.Text = COMMENT_TEXT

			return XmlUtil.serialize(Remarks)
		}

		return xml
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
