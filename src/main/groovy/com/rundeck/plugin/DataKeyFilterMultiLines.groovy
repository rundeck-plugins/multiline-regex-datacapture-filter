package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.workflow.OutputContext
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.databind.ObjectMapper

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Plugin(name = DataKeyFilterMultiLines.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Multiline Regex Data Capture',
        description = '''Captures Multiline Regex Key/Value data using a simple text format
from a regular expression.
''')
class DataKeyFilterMultiLines implements LogFilterPlugin{
    public static final String PROVIDER_NAME = 'key-value-data-multilines'
    public static final String PATTERN = '^(.+?)\\s*=\\s*(.+)'

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression for matching key/value data.
The regular expression must define two Capturing Groups. The first group matched defines
the data key, and the second group defines the data value. If it just capture one group, the name parameter must be defined.
See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            defaultValue = DataKeyFilterMultiLines.PATTERN,
            required = true,
            validatorClass = DataKeyFilterMultiLines.RegexValidator
    )
    String regex

    @PluginProperty(
            title = 'Name Data',
            description = '''If only one groups is provided, the name of the captured variable'''
    )
    String name

    @PluginProperty(
            title = 'Hide Output',
            description = '''If true, log output will be hidden''',
            defaultValue = 'false'
    )
    Boolean hideOutput

    @PluginProperty(
            title = 'Log Data',
            description = '''If true, log the captured data''',
            defaultValue = 'false'
    )
    Boolean logData

    static class RegexValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                def compile = Pattern.compile(value)
                return true
            } catch (PatternSyntaxException e) {
                throw new ValidationException(e.message, e)
            }
        }
    }

    Pattern dataPattern;
    OutputContext outputContext
    Map<String, String> allData
    private ObjectMapper mapper
    private StringBuffer buffer

    @Override
    void init(final PluginLoggingContext context) {
        dataPattern = Pattern.compile(regex, Pattern.DOTALL)
        outputContext = context.getOutputContext()
        mapper = new ObjectMapper()
        allData = [:]
        buffer = new StringBuffer()
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && event.loglevel == LogLevel.NORMAL && event.message?.length() > 0) {
            buffer.append(event.message).append(System.getProperty("line.separator"))

            if(hideOutput){
                event.loglevel = LogLevel.DEBUG
            }

        }
    }

    @Override
    void complete(final PluginLoggingContext context) {

        if(buffer.size()>0){
            Matcher match = dataPattern.matcher(buffer.toString())

            if (match.matches()) {
                def key,value

                if(match.groupCount()>0){
                    if(match.groupCount()==1 && name){
                        key = name
                        value = match.group(1)
                    }else {
                        if(match.groupCount()>1){
                            key = match.group(1)
                            value = match.group(2)
                        }
                    }

                    if (key && value) {
                        allData[key] = value
                        outputContext.addOutput("data", key, value)
                    }
                }
            }

            if (logData) {
                context.log(
                        2,
                        mapper.writeValueAsString(allData),
                        [
                                'content-data-type'       : 'application/json',
                                'content-meta:table-title': 'Key Value Data: Results'
                        ]
                )
            }
        }

    }
}
