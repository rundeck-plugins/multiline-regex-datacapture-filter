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
    def "multiline-filter-ok: #testName"() {
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
        dolog | name | regex                          | lines                           | expect                                    | testName
        true  | null | DataKeyFilterMultiLines.PATTERN       | ['some_list = first_element',
                                                                'second_element',
                                                                'third_element',
                                                                'fourth_element'
        ] |
        [some_list: 'first_element\n' +
                    'second_element\n' +
                    'third_element\n' +
                    'fourth_element\n'
        ]                                                                                                                         | "default key=value pattern"
        true  | "log" | "^\\s*User Comments:\\s*(.*)" | ['User Comments: This is a',
                                                         'test',
                                                         'message',
                                                         '',
                                                         'done']  |
                                                        [log: 'This is a\n' +
                                                          'test\n' +
                                                          'message\n' +
                                                          'done\n']                                            | "user comments with named capture"




    }

    @Unroll
    def "multiline-filter-nok: #testName"() {
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
        dolog | name | regex                          | lines                           | expect | testName
        true  | null | "^\\s*User Comments:\\s*(.*)" | ['User Comments: This is a',
                                                         'test',
                                                         'message',
                                                         '',
                                                         'done']  | null    | "single capture group without name parameter"
    }

    @Unroll
    def "multiline-filter-multiple-key-value-pairs: #testName"() {
        given:
        def plugin = new DataKeyFilterMultiLines()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.name = name
        plugin.captureMultipleKeysValues = true
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
        dolog | name | regex                         | lines    | expect                                               | testName
        true  | null | "(\\w+)\\s(\\d+)" | ['hello 1',
                                                        'world 2',
                                                        'test 456',
                                                        '',
                                                        'done'] | [test: "456", hello: "1", world: "2"]              | "basic two-group capture"


        true  | "any_name" | "(\\w+\\s\\d+)" | ['hello 1',
                                            'world 2',
                                            'test 456',
                                            '',
                                            'done'] | ["any_name": "hello 1\nworld 2\ntest 456"]          | "single group with name parameter"

        true  | null | "(\\w+\\s\\d+)" | ['hello 1',
                                            'world 2',
                                            'test 456',
                                            '',
                                            'done'] | null                                                | "single group without name parameter"

        // Test AWS SSM behavior: multiple lines arriving together in a single event
        true  | null | "(\\w+)\\s(\\d+)" | ['hello 1\nworld 2\ntest 456\n\ndone'] | [test: "456", hello: "1", world: "2"]              | "SSM multiline in single event"

        // Test mixed behavior: some lines together, some separate
        true  | null | "(\\w+)\\s(\\d+)" | ['hello 1\nworld 2', 'test 456', 'done'] | [test: "456", hello: "1", world: "2"]              | "mixed multiline and separate events"
    }

    @Unroll
    def "multiline-filter-complex-regex-patterns: #testName"() {
        given:
        def plugin = new DataKeyFilterMultiLines()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.name = name
        plugin.captureMultipleKeysValues = captureMultiple
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

        where:
        dolog | name | captureMultiple | regex                                                                  | lines                                                          | expect                                                     | testName

        // Test timestamp parsing with multiple groups (only first 2 groups used as key-value)
        true  | null | true | "(\\d{4}-\\d{2}-\\d{2})\\s(\\d{2}:\\d{2}:\\d{2})\\s([A-Z]+)\\s(.+)"  | ['2023-11-06 14:30:15 INFO Application started',
                                                                                                         '2023-11-06 14:30:16 ERROR Database connection failed'] |
                                                                                                        ['2023-11-06': '14:30:15\n14:30:16']              | "timestamp parsing with log levels"

        // Test file path parsing with escaped characters
        true  | null | true | "File:\\s+(.+\\.log)\\s+Size:\\s+(\\d+)" | ['File: /var/log/app.log Size: 1024',
                                                                           'File: /tmp/debug.log Size: 512'] |
                                                                          ['/var/log/app.log': '1024', '/tmp/debug.log': '512']   | "file path and size parsing"

        // Test JSON-like parsing
        true  | null | true | '"([^"]+)":\\s*"([^"]+)"' | ['"name": "John Doe"',
                                                            '"email": "john@example.com"',
                                                            '"status": "active"'] |
                                                           [name: 'John Doe', email: 'john@example.com', status: 'active'] | "JSON key-value parsing"

        // Test IP address and port parsing
        true  | null | true | "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)" | ['192.168.1.100:8080',
                                                                                        '10.0.0.1:3306',
                                                                                        '127.0.0.1:5432'] |
                                                                                       ['192.168.1.100': '8080', '10.0.0.1': '3306', '127.0.0.1': '5432'] | "IP address and port extraction"

        // Test case-insensitive matching (using (?i) flag)
        true  | null | true | "(?i)(ERROR|WARN|INFO)\\s+(.+)" | ['ERROR Database connection failed',
                                                                  'warn Memory usage high',
                                                                  'Info Application started'] |
                                                                 [ERROR: 'Database connection failed', warn: 'Memory usage high', Info: 'Application started'] | "case-insensitive log level matching"

        // Test complex log format parsing
        true  | null | true | "\\[([^\\]]+)\\]\\s+(\\w+):\\s+(.+)" | ['[2023-11-06 14:30:15] INFO: System startup completed',
                                                                      '[2023-11-06 14:30:16] ERROR: Database timeout'] |
                                                                     ['2023-11-06 14:30:15': 'INFO', '2023-11-06 14:30:16': 'ERROR'] | "structured log format with brackets"

        // Test URL parameter parsing
        true  | null | true | "([^=]+)=([^&\\s]+)" | ['user=admin',
                                                      'session=abc123',
                                                      'timeout=300'] |
                                                     [user: 'admin', session: 'abc123', timeout: '300']  | "URL parameter parsing"

        // Test multiline with newlines in single event (like SSM) with complex regex
        true  | null | true | "STATUS\\s+(\\w+)\\s+MSG\\s+(.+)" | ['STATUS SUCCESS MSG Operation completed\nSTATUS FAILED MSG Connection timeout\nSTATUS PENDING MSG Waiting for response'] |
                                                                   [SUCCESS: 'Operation completed', FAILED: 'Connection timeout', PENDING: 'Waiting for response'] | "status message parsing in single event"

        // Test with single group and name parameter for simple metric extraction
        true  | "cpu_usage" | true | "CPU:\\s+(\\d+)%" | ['CPU: 85%',
                                                           'Some other line',
                                                           'CPU: 92%'] |
                                                          ['cpu_usage': '85\n92']                         | "CPU metric extraction with name"

        // Test email parsing with complex domains
        true  | null | true | "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})" | ['user@example.com',
                                                                                         'admin@sub.domain.org',
                                                                                         'test.user+tag@company.co.uk'] |
                                                                                        ['user': 'example.com', 'admin': 'sub.domain.org', 'test.user+tag': 'company.co.uk'] | "email address domain extraction"
    }
}
