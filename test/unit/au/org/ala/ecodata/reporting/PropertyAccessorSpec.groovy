package au.org.ala.ecodata.reporting

import spock.lang.Specification

/**
 * Specification for the PropertyAccessor
 */
class PropertyAccessorSpec extends Specification {

    def "the PropertyAccessor can unroll a list"() {
        setup:
        PropertyAccessor propertyAccessor = new PropertyAccessor("list.property")
        Map data = [list:[[property:1],[property:2],[property:3],[property:4],[property:5]], extra:1]

        when:
        List<Map> unrolledData = propertyAccessor.unroll(data)

        then:
        unrolledData == [[list:[property:1], extra:1], [list:[property:2], extra:1], [list:[property:3], extra:1], [list:[property:4], extra:1], [list:[property:5], extra:1]]
    }
    
    def "the PropertyAccessor can unroll a nested list"() {
        setup:
        PropertyAccessor propertyAccessor = new PropertyAccessor("top.list.property")
        Map data = [top:[list:[[property:1],[property:2],[property:3],[property:4],[property:5]], extra:1], extra:2]

        when:
        List<Map> unrolledData = propertyAccessor.unroll(data)

        then:
        unrolledData == [[top:[list:[property:1], extra:1], extra:2], [top:[list:[property:2], extra:1], extra:2], [top:[list:[property:3], extra:1], extra:2], [top:[list:[property:4], extra:1], extra:2], [top:[list:[property:5], extra:1], extra:2]]

    }

    def "the PropertyAccessor can unroll lists nested inside lists"() {

        setup:
        PropertyAccessor propertyAccessor = new PropertyAccessor("list1.list2.property")
        Map data = [
                list1:[
                        [
                                list2:[ [property:1], [property:2] ],
                                extra:1
                        ],
                        [
                                list2:[ [property:3] ],
                                extra:2
                        ],
                        [
                                list2:[ [property:1], [property:4], [property:5] ],
                                extra:3
                        ],
                        [
                                list2:[ [property:4] ],
                                extra:4
                        ]
                ],
                extra:2]

        when:
        List<Map> unrolledData = propertyAccessor.unroll(data)

        then:
        unrolledData == [
                [list1:[list2:[property:1], extra:1], extra:2], [list1:[list2:[property:2], extra:1], extra:2],
                [list1: [list2:[property:3], extra:2], extra:2],
                [list1:[list2:[property:1], extra:3], extra:2], [list1:[list2:[property:4], extra:3], extra:2], [list1:[list2:[property:5], extra:3], extra:2],
                [list1: [list2:[property:4], extra:4], extra:2]
        ]

    }

}
