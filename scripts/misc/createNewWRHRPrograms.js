load("UUID.js");
function createOrUpdateProgram(name, acronym, startDate, endDate, outcomes, parentId, config) {

    var programCursor = db.program.find({name:name});
    var now = ISODate();
    if (programCursor.hasNext()) {
        var program = programCursor.next();
        program.lastUpdated = now;
        program.config = config;
        program.acronym = acronym;
        program.outcomes = outcomes;
        program.startDate = startDate;
        program.endDate = endDate;
        program.parent = parentId;

        db.program.save(program);
    }
    else {
        program = {
            name: name,
            programId: UUID.generate(),
            dateCreated: now,
            lastUpdated: now,
            startDate: startDate,
            parent: null,
            endDate: endDate,
            acronym: acronym,
            status: 'active',
            outcomes: outcomes,
            config: config
        };

        if (parentId) {
            program.parent = parentId;
        }

        db.program.insert(program);

    }

    return db.program.find({programId:program.programId}).next();
}

var bushfireParent;
var program = db.program.find({name:'Bushfire Wildlife and Habitat Recovery'});
if (program.hasNext()) {
    bushfireParent = program.next();
}
else {
    bushfireParent = createOrUpdateProgram("Bushfire Wildlife and Habitat Recovery",  "", ISODate("2019-11-30T13:00:00Z"), ISODate("2020-11-30T13:00:00Z"), []);
}

var cvaConfig = {

    "meriPlanContents" : [
        "objectivesList",
        "monitoringIndicators",
        "projectImplementation",
        "projectPartnerships",
        "keq",
        "meriBudget",
        "risksAndThreats"
    ],
    "visibility" : "public",
    "requiresActivityLocking" : true,
    "navigationMode" : "returnToProject",
    "projectTemplate" : "rlp",
    "activityPeriodDescriptor" : "Outputs report #",
    "meriPlanTemplate" : "configurableMeriPlan",
    "riskAndThreatTypes" : [
        "Performance",
        "Work Health and Safety",
        "People resources",
        "Financial",
        "External stakeholders",
        "Natural Environment"
    ],
    "projectReports" : [
        {
            "reportType" : "Activity",
            "reportDescriptionFormat" : "Progress Report %1d",
            "reportNameFormat" : "Progress Report %1d",
            "description" : "",
            "category" : "Progress Reports",
            "activityType" : "Wildlife Recovery Progress Report",
            "reportsAlignedToCalendar" : false,
            "endDates" : [
                "2020-04-14T14:00:00Z",
                "2020-07-14T14:00:00Z",
                "2021-01-14T13:00:00Z"
            ],
            "canSubmitDuringReportingPeriod" : true
        },
        {
            "reportType" : "Single",
            "firstReportingPeriodEnd" : "2021-06-30T14:00:00Z",
            "reportDescriptionFormat" : "Final Report",
            "reportNameFormat" : "Final Report",
            "reportingPeriodInMonths" : 18,
            "multiple" : false,
            "description" : "",
            "category" : "Final Report",
            "reportsAlignedToCalendar" : false,
            "activityType" : "Final Report"
        }
    ],
    objectives:[
        "Develop a central point nationally for volunteers to register their interest in participating in environmental restoration work in bushfire affected areas",
        "Develop a central point for organisations to identify the need for volunteers to undertake environmental restoration work in bushfire affected areas",
        "Develop work health and safety protocols to keep volunteers safe"
    ]
};

//createOrUpdateProgram("CVA", "", program.startDate, program.endDate, cvaOutcomes, program._id, cvaConfig)

var gaConfig = {

    "meriPlanContents" : [
        "objectivesList",
        "monitoringIndicators",
        "projectImplementation",
        "projectPartnerships",
        "keq",
        "meriBudget",
        "risksAndThreats"
    ],
        "visibility" : "public",
        "requiresActivityLocking" : true,
        "navigationMode" : "returnToProject",
        "projectTemplate" : "rlp",
        "activityPeriodDescriptor" : "Outputs report #",
        "meriPlanTemplate" : "configurableMeriPlan",
        "riskAndThreatTypes" : [
            "Performance",
            "Work Health and Safety",
            "People resources",
            "Financial",
            "External stakeholders",
            "Natural Environment"
        ],
        "projectReports" : [
        {
            "reportType" : "Activity",
            "reportDescriptionFormat" : "Progress Report %1d",
            "reportNameFormat" : "Progress Report %1d",
            "description" : "",
            "category" : "Progress Reports",
            "activityType" : "Wildlife Recovery Progress Report",
            "reportsAlignedToCalendar" : false,
            "endDates" : [
                "2020-04-14T14:00:00Z",
                "2020-07-14T14:00:00Z",
                "2021-01-14T13:00:00Z"
            ],
            "canSubmitDuringReportingPeriod" : true
        },
        {
            "reportType" : "Single",
            "firstReportingPeriodEnd" : "2021-06-30T14:00:00Z",
            "reportDescriptionFormat" : "Final Report",
            "reportNameFormat" : "Final Report",
            "reportingPeriodInMonths" : 18,
            "multiple" : false,
            "description" : "",
            "category" : "Final Report",
            "reportsAlignedToCalendar" : false,
            "activityType" : "Final Report"
        }
    ],
    objectives:[
        "Understand the native seed supply needs to restore vegetation and wildlife habitat across fire impacted regions",
        "Build the capacity of the native seed and nursery industry through co-ordination of seed collection activities in bushfire affected areas and other vulnerable landscapes",
        "Develop and coordinate a ten-year native seed and landscape restoration strategy including engagement with key stakeholders",
        "Determine the scope of Florabank as the National tool to coordinate, manage and record seed collection activity in consultation with the Steering Committee",
        "In collaboration with existing resources and expertise, support conservation seed banks for future safeguarding against local extinctions and adaptation",
        "Develop and utilise national native seed capacity and employment/training opportunities, with particular emphasis on Indigenous and local communities",
        "Keep participants safe, with particular regard to working in or near fire-grounds",
        "Establish sound governance arrangements in consultation with the Department to ensure proper use of funds (efficient, effective, economical, and ethical)"
    ]
};
var gaOutcomes = [];

createOrUpdateProgram("GA", "", bushfireParent.startDate, bushfireParent.endDate, gaOutcomes, bushfireParent._id, gaConfig);

var wrrOutcomes = [
];
var wrrConfig = {

    "meriPlanContents": ["objectivesList",  "activities"],
    "visibility": "public",
    "requiresActivityLocking": true,
    "navigationMode": "returnToProject",
    "projectTemplate": "rlp",
    "activityPeriodDescriptor": "Outputs report #",
    "meriPlanTemplate": "configurableMeriPlan",
    "riskAndThreatTypes": [
    "Performance",
    "Work Health and Safety",
    "People resources",
    "Financial",
    "External stakeholders",
    "Natural Environment"
],
    "projectReports": [
    {
        "reportType": "Activity",
        "reportDescriptionFormat": "Progress Report %1d",
        "reportNameFormat": "Progress Report %1d",
        "description": "",
        "category": "Progress Reports",
        "activityType": "Wildlife Recovery Progress Report",
        "reportsAlignedToCalendar": false,
        "endDates": [
            "2020-04-14T14:00:00Z",
            "2020-07-14T14:00:00Z",
            "2021-01-14T13:00:00Z"
        ],
        "canSubmitDuringReportingPeriod": true
    },
    {
        "reportType": "Single",
        "firstReportingPeriodEnd": "2021-06-30T14:00:00Z",
        "reportDescriptionFormat": "Final Report",
        "reportNameFormat": "Final Report",
        "reportingPeriodInMonths": 18,
        "multiple": false,
        "description": "",
        "category": "Final Report",
        "reportsAlignedToCalendar": false,
        "activityType": "Final Report"
    }
],
    services: [
        {name:"Post-bushfire native wildlife rescue, treatment, rehabilitation, transfer and re-introduction into suitable environments", id:34},
        {name:"Provision of supplementary food, water and shelter in situ to post bushfire affected native wildlife to support their survival", id:35},
        {name:"New, upgraded or extended facilities and equipment for treating and housing rescued native wildlife ", id:36},
        {name:"Emergency interventions to support at risk threatened species affected by bushfire, such as conservation breeding programs for potential future re-introductions", id:37},
        {name:"Education, training and reference resources to improve the knowledge and skills of wildlife rehabilitators and veterinarians providing native wildlife rehabilitation and conservation services", id:38},
        {name:"Training, development of policies and procedures, personal protective equipment and support services to keep wildlife rehabilitators physically and mentally safe and well", id:39},
        {name:"Communications, including a wildlife rescue hotline and website presence", id:40},
        {name:"Administrative costs and professional services necessary to ensure proper management of funds.", id:41}
    ],
    objectives:[
        "Rescue and rehabilitate displaced, orphaned, sick and injured native wildlife",
        "Reduce the risk of extinctions through emergency interventions",
        "Provide supplementary food, water and shelter in situ to starving, dehydrated or vulnerable native wildlife",
        "Increase the capacity and preparedness of wildlife rehabilitators and their organisations to respond to emergencies",
        "Keep wildlife rehabilitators physically and mentally safe and well"
    ]

};
createOrUpdateProgram("Wildlife Rescue and Rehabilitation", "WRR", bushfireParent.startDate, bushfireParent.endDate, wrrOutcomes, bushfireParent._id, wrrConfig);

// Create an ERF sub-program "Direct Source Procurement"
var erf = db.program.find({acronym:'ERF'}).next();
var directSourceProcurement = createOrUpdateProgram("Direct source procurement", "", erf.startDate, erf.endDate, erf.outcomes, erf._id, null);
db.project.update({programId:erf.programId}, {$set:{programId:directSourceProcurement.programId}}, {multi:true});

var emergencyInterventionConfig = {
    "visibility" : "public",
    "requiresActivityLocking" : true,
    "navigationMode" : "returnToProject",
    "projectTemplate" : "rlp",
    "activityPeriodDescriptor" : "Outputs report #",
    "meriPlanTemplate" : "configurableMeriPlan",
    "riskAndThreatTypes" : [
        "Performance",
        "Work Health and Safety",
        "People resources",
        "Financial",
        "External stakeholders",
        "Natural Environment"
    ],
    "projectReports" : [
        {
            "reportType" : "Activity",
            "reportDescriptionFormat" : "Progress Report %1d",
            "reportNameFormat" : "Progress Report %1d",
            "description" : "",
            "category" : "Progress Reports",
            "activityType" : "Wildlife Recovery Progress Report",
            "reportsAlignedToCalendar" : false,
            "endDates" : [
                "2020-04-14T14:00:00Z",
                "2020-07-14T14:00:00Z",
                "2021-01-14T13:00:00Z"
            ],
            "canSubmitDuringReportingPeriod" : true
        },
        {
            "reportType" : "Single",
            "firstReportingPeriodEnd" : "2021-06-30T14:00:00Z",
            "reportDescriptionFormat" : "Final Report",
            "reportNameFormat" : "Final Report",
            "reportingPeriodInMonths" : 18,
            "multiple" : false,
            "description" : "",
            "category" : "Final Report",
            "reportsAlignedToCalendar" : false,
            "activityType" : "Final Report"
        }]
};

var emergencyIntervention = createOrUpdateProgram("Emergency Intervention", "", ISODate("2020-06-30T14:00:00Z"), ISODate("2021-06-30T14:00:00Z"), [], null, emergencyInterventionConfig);
var stateGrants = createOrUpdateProgram("State Intervention", "", emergencyIntervention.startDate, emergencyIntervention.endDate, [], emergencyIntervention._id, emergencyInterventionConfig);

