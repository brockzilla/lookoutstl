package com.lookoutstl;

import java.util.Date;
import java.text.DateFormat;
import java.util.Collection;
import java.util.regex.Pattern;
import javax.mail.internet.InternetAddress;

import org.jboss.resteasy.logging.Logger;

/** I validate things */
public class Validator {
    private static Logger log = Logger.getLogger(Validator.class);

    public static boolean isWack(Object obj) {
        if (obj == null) {
            return true;
        } else {
            if (obj instanceof String) {
                return isWack((String)obj);
            } else if (obj instanceof Integer) {
                return isWack((Integer)obj);
            } else if (obj instanceof Long) {
                return isWack((Long)obj);
            } else if (obj instanceof Double) {
                return isWack((Double)obj);
            } else if (obj instanceof String[]) {
                return isWack((String[])obj);
            } else if (obj instanceof Collection) {
                return isWack((Collection)obj);
            } else if (obj instanceof Date) {
                return isWack((Date)obj);
            } else {
                return false;
            }
        }
    }

    public static boolean isCool(Object obj) {
        return !isWack(obj);
    }

    public static boolean isCool(Integer pInteger) {
        return !isWack(pInteger);
    }

    public static boolean isWack(Integer pInteger) {
        boolean retVal = true;
        if (pInteger != null) {
            retVal = isWack(pInteger.intValue());
        }
        return retVal;
    }

    public static boolean isWack(Long pNum) {
        return pNum == null || isWack(pNum.longValue());
    }

    public static boolean isWack(Double pDouble) {
        return pDouble == null || isWack(pDouble.doubleValue());
    }

    public static boolean isWack(String pString) {
        if ((pString == null) ||
            (pString.trim().length() <= 0) ||
            (pString.trim().equals("null"))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCool(String pString) {
        return !isWack(pString);
    }

    public static boolean isWack(String[] pStringArray) {
        boolean wack = false;
        if (pStringArray == null || pStringArray.length == 0) {
            wack = true;
        } else {
            for (int i=0; i<pStringArray.length; i++) {
                wack = (wack || Validator.isWack(pStringArray[i]));
            }
        }
        return wack;
    }

    public static boolean isCool(String[] pStringArray) {
        return !isWack(pStringArray);
    }

    public static boolean isWack(long pNum) {
        if (pNum < 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCool(long pNum) {
        return !isWack(pNum);
    }

    public static boolean isWack(int pInt) {
        if (pInt < 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCool(int pInt) {
        return !isWack(pInt);
    }

    public static boolean isWack(boolean pBoolean) {
        return false;
    }

    public static boolean isCool(boolean pBoolean) {
        return !isWack(pBoolean);
    }

    public static boolean isWack(Date pDate) {
        // could use more here
        if (pDate == null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCool(Date pDate) {
        return !isWack(pDate);
    }

    public static boolean isWack(double pDouble) {
        if (pDouble < 0.0d) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCool(double pDouble) {
        return !isWack(pDouble);
    }

    public static boolean isWack(Collection pCollection) {
        if (pCollection != null && pCollection.size() > 0) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isCool(Collection pCollection) {
        return !isWack(pCollection);
    }

    public static boolean isString(String pInput) {
        return true;
    }

    public static boolean isInt(String pInput) {
        try {
            int test = new Integer(pInput).intValue();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isBoolean(String pInput) {
        try {
            boolean test = new Boolean(pInput).booleanValue();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDate(String pInput) {
        try {
            Date test = DateFormat.getInstance().parse(pInput);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String pInput) {
        try {
            double test = new Double(pInput).doubleValue();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isNonZeroNumber(String pInput) {
        boolean nonZero = false;
        try {
            double test = new Double(pInput).doubleValue();
            if (test > 0.0) {
                nonZero = true;
            }
        } catch (Exception e) {}
        return nonZero;
    }

    public static boolean isEmailAddress(String pInput) {
        boolean result = true;
        try {
            result = isEmailAddress(new InternetAddress(pInput));
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public static boolean isEmailAddress(InternetAddress pInput) {
        boolean result = true;
        try {
            pInput.validate();
        } catch (Exception e) {
            result = false;
        }
        return result;
    }
}
