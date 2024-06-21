package com.payment.app.ui.pinpad;

/**
 * @author GuoJirui.
 * @date 2021/4/27.
 * @desc
 */
public interface IPinpadCode {

    //Define the return code
    int PINPAD_SUCCESS = 0;
    int PINPAD_ERROR = -1;
    int PINPAD_CANCEL = -2;
    int PINPAD_TIMEOUT = -3;
    int PINPAD_EXCEPTION = -4;
    int PINPAD_SCREEN_OFF = -5;
    int PINPAD_UNKNOWN = -99;
    int USER_TURN_OFF = -100;
    int OTHER_UNKNOWN = -199;
    //Defines a constant for the name of the extra data
    String PINPAD_BACK_CODE = "pinpad_code";
    String PINPAD_BACK_DATA = "pinpad_data";

    byte PIN_BYPASS = 1;            //The user confirmed and did not enter the PIN on time
    byte PIN_NORMAL = 2;            //User entered PIN
    byte PIN_TIMEOUT = 3;           //timeout
    byte OFFLINE_PIN_EXCEED_LIMIT = 4;        //Offline PIN exceeds limit
    byte OFFLINE_PIN_6983 = 5;        //Check offline PIN fetch random number and verify command error

}
