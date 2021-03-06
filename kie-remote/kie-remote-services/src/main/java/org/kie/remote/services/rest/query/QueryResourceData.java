package org.kie.remote.services.rest.query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.kie.api.task.model.Status;
import org.kie.remote.services.rest.exception.KieRemoteRestOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResourceData {

    // @formatter:off
    static final String [] generalQueryParams = { 
        "processinstanceid", "processid", "workitemid",
        "deploymentid"
    };
    
    static final String [] generalQueryParamsShort = { 
        "piid", "pid", "wid",
        "did"
    };
    
    static final String [] taskQueryParams = { 
        "taskid", 
        "initiator", "stakeholder", "potentialowner", "taskowner", "businessadmin",
        "taskstatus"
    };
    
    static final String [] taskQueryParamsShort = { 
       "tid", 
       "init", "stho", "po", "to", "ba",
       "tst"
    };
    
    static final String [] procInstQueryParams = { 
        "processinstancestatus", 
        "processversion", 
        "startdate", "enddate"
    };

    static final String [] procInstQueryParamsShort = { 
        "pist",
        "pv", 
        "stdt", "edt"
    };

    static final String [] varInstQueryParams = { 
       "varid", "varvalue", 
       "var", "varregex",
       "all"
    };
    
    static final String [] varInstQueryParamsShort = { 
       "vid", "vv", 
       null, "vr",
       null
    };
    
    public static final String [] metaRuntimeParams = { 
       "memory", "history"
    };
    
    public static final String [] metaRuntimeParamsShort = { 
       "mem", "hist"
    };
    // @formatter:on

    public static final Map<Integer, String> actionParamNameMap = new ConcurrentHashMap<Integer, String>();
    public static final Map<String, Integer> paramNameActionMap = new ConcurrentHashMap<String, Integer>();
    
    protected static final int GENERAL_END = 4;
    protected static final int TASK_END = 11;
    protected static final int PROCESS_END = 15;
    protected static final int VARIABLE_END = 20;
    protected static final int META_END = 22;
    
    static { 
        // there are faster ways to do this, but the checks here are very valuable
        int idGen = 0;
        idGen = addParamsToActionMap(idGen, generalQueryParams, generalQueryParamsShort);
        if( idGen != GENERAL_END ) { 
            throw new IllegalStateException("General query parameters [" + idGen + "]");
        }
        idGen = addParamsToActionMap(idGen, taskQueryParams, taskQueryParamsShort);
        if( idGen != TASK_END ) { 
            throw new IllegalStateException("Task query parameters [" + idGen + "]" );
        }
        idGen = addParamsToActionMap(idGen, procInstQueryParams, procInstQueryParamsShort);
        if( idGen != PROCESS_END ) { 
            throw new IllegalStateException("Process instance query parameters [" + idGen + "]" );
        }
        idGen = addParamsToActionMap(idGen, varInstQueryParams, varInstQueryParamsShort);
        if( idGen != VARIABLE_END ) { 
            throw new IllegalStateException("Variable instance query parameters [" + idGen + "]" );
        }
        idGen = addParamsToActionMap(idGen, metaRuntimeParams, metaRuntimeParamsShort);
        if( idGen != META_END ) { 
            throw new IllegalStateException("Meta parameters [" + idGen + "]" );
        }
    }
    
    private static int addParamsToActionMap(int idGen, String [] params, String [] paramsShort ) { 
        if( params.length != paramsShort.length ) { 
            throw new IllegalStateException( params.length + " params but " + paramsShort.length + " abbreviated params!");
        }
        for( int i = 0; i < params.length; ++i ) { 
            int id = idGen++;
            paramNameActionMap.put(params[i], id);
            actionParamNameMap.put(id, params[i]);
            if( paramsShort[i] != null ) { 
                paramNameActionMap.put(paramsShort[i], id);
            }
        }
        return idGen;
    }
 
    private static Set<String> queryParameters = new HashSet<String>();
    
    public static Set<String> getQueryParameters() { 
        synchronized(queryParameters) { 
            if( queryParameters.isEmpty() ) { 
                String [][] queryParamArrs = { 
                        generalQueryParams, generalQueryParamsShort,
                        taskQueryParams, taskQueryParamsShort,
                        procInstQueryParams, procInstQueryParamsShort,
                        varInstQueryParams, varInstQueryParamsShort
                      // metaRuntimeParams, metaRuntimeParamsShort
                };
                for( String [] paramArr : queryParamArrs ) { 
                    for( String param : paramArr ) { 
                       if( param != null ) { 
                           queryParameters.add(param);
                       }
                    }
                }
            }
        }
        return queryParameters;
    }
    
    // "get" parser methods -------------------------------------------------------------------------------------------------------
    
    public static long [] getLongs(int action, String [] data) { 
        String name = actionParamNameMap.get(action);
        long [] result = new long[data.length];
        int i = 0;
        try { 
           for( ; i < data.length; ++i) { 
              result[i] = Long.valueOf(data[i]); 
           }
        } catch( NumberFormatException nfe ) { 
            throw KieRemoteRestOperationException.badRequest("Values for query parameter '" + name + "' must be long (" + data[i] + ")");
        }
        return result;
    }
    
    public static int [] getInts(int action, String [] data) { 
        String name = actionParamNameMap.get(action);
        int [] result = new int[data.length];
        int i = 0;
        try { 
           for( ; i < data.length; ++i) { 
              result[i] = Integer.valueOf(data[i]); 
           }
        } catch( NumberFormatException nfe ) { 
            throw KieRemoteRestOperationException.badRequest("Values for query parameter '" + name + "' must be integers (" + data[i] + ")");
        }
        return result;
    }
  
    public static final SimpleDateFormat QUERY_PARAM_DATE_FORMAT 
        = new SimpleDateFormat("yy-MM-dd_HH:mm:ss");
    
    public static Date [] getDates(int action, String [] data) { 
        Date [] result = new Date[data.length];
        for( int i = 0; i < data.length; ++i) { 
            result[i] = parseDate(data[i]);
        }
        return result;
    }
   
    private static Date parseDate(String dateStr) { 
        String [] parts = dateStr.split("_");
        String [] dateParts = null;
        String [] timeParts = null;
        String parseDateStr = null;
        if( parts.length == 2 ) { 
            dateParts = parts[0].split("-");
            if( dateParts.length != 3 ) { 
                badDateString(dateStr);
            }
            timeParts = parts[1].split(":");
            if( timeParts.length != 3 ) { 
                badDateString(dateStr);
            }
            parseDateStr = dateStr;
        } else if( parts.length == 1 ) { 
            dateParts = parts[0].split("-");
            timeParts = parts[0].split(":");
            if( timeParts.length == 3 && dateParts.length == 1 ) { 
                dateParts = QUERY_PARAM_DATE_FORMAT.format(new Date()).split("_")[0].split("-");
            } else if( dateParts.length == 3 && timeParts.length == 1 ) { 
               String [] newTimeParts = { "0", "0", "0" };
               timeParts = newTimeParts;
            } else { 
                badDateString(dateStr);
            }
        } else { 
            badDateString(dateStr);
        }
        if( parseDateStr == null ) { 
            StringBuilder tmpStr = new StringBuilder(dateParts[0]);
            for( int i = 1; i < 3; ++i ) { 
               tmpStr.append("-").append(dateParts[i]);
            }
            tmpStr.append("_");
            tmpStr.append(timeParts[0]);
            for( int i = 1; i < 3; ++i ) { 
               tmpStr.append(":").append(timeParts[i]);
            }
            parseDateStr = tmpStr.toString();
        }
        try {
            return QUERY_PARAM_DATE_FORMAT.parse(parseDateStr);
        } catch( ParseException pe ) {
            badDateString(parseDateStr);
            throw new RuntimeException("This should never be thrown", pe);
        }
    }
   
    private static void badDateString(String dateStr) { 
        throw KieRemoteRestOperationException.badRequest("'" + dateStr + "' is not a valid format for a date value query parameter.");
    }

    public static Status [] getTaskStatuses(String [] data) { 
       Status [] result =  new Status[data.length];
       for( int i = 0; i < data.length; ++i ) { 
          if( data[i].toLowerCase().equals(Status.InProgress.toString().toLowerCase()) ) { 
             result[i] = Status.InProgress; 
          } else { 
              String value = data[i].substring(0, 1).toUpperCase() + data[i].substring(1);
              try { 
                 result[i] = Status.valueOf(value);
              } catch( Exception e ) { 
                 throw KieRemoteRestOperationException.badRequest("Task status '" + data[i] + "' is not valid.");
              }
          }
       }
       return result;
    }
}
