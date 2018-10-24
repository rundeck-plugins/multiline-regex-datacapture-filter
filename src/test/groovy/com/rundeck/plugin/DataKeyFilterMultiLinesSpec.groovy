package com.rundeck.plugin

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import spock.lang.Specification
import spock.lang.Unroll
import com.dtolabs.rundeck.core.execution.workflow.DataOutput

class DataKeyFilterMultiLinesSpec extends Specification {

    @Unroll
    def "multiline-filter-ok"() {
        given:
        def plugin = new DataKeyFilterMultiLines()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.name = name
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        lines.each { line ->
            events << Mock(LogEventControl) {
                getMessage() >> line
                getEventType() >> 'log'
                getLoglevel() >> LogLevel.NORMAL
            }
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)
        then:

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == (expect ? ['data': expect] : null)
        if (expect) {
            if (dolog) {
                1 * context.log(2, _, _)
            } else {
                0 * context.log(*_)
            }
        }


        where:
        dolog | name | regex                          | lines                           | expect
        true  | null | DataKeyFilterMultiLines.PATTERN       | ['some_list = first_element',
                                                                'second_element',
                                                                'third_element',
                                                                'fourth_element'
        ] |
        [some_list: 'first_element\n' +
                    'second_element\n' +
                    'third_element\n' +
                    'fourth_element\n'
        ]
        true  | "log" | "^\\s*User Comments:\\s*(.*)" | ['User Comments: This is a',
                                                         'test',
                                                         'message',
                                                         '',
                                                         'done']  |
                                                        [log: 'This is a\n' +
                                                          'test\n' +
                                                          'message\n' +
                                                          'done\n']




    }

    @Unroll
    def "multiline-filter-nok"() {
        given:
        def plugin = new DataKeyFilterMultiLines()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.name = name
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        lines.each { line ->
            events << Mock(LogEventControl) {
                getMessage() >> line
                getEventType() >> 'log'
                getLoglevel() >> LogLevel.NORMAL
            }
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)
        then:

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == (expect ? ['data': expect] : null)
        if (expect) {
            if (dolog) {
                1 * context.log(2, _, _)
            } else {
                0 * context.log(*_)
            }
        }


        //case when the name is not set and the pattern doesn't capture a key
        where:
        dolog | name | regex                          | lines                           | expect
        true  | null | "^\\s*User Comments:\\s*(.*)" | ['User Comments: This is a',
                                                         'test',
                                                         'message',
                                                         '',
                                                         'done']  | null
    }
}
