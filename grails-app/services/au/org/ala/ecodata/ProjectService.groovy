package au.org.ala.ecodata

import au.org.ala.ecodata.converter.SciStarterConverter
import au.org.ala.ecodata.reporting.Score
import grails.converters.JSON

import java.lang.reflect.UndeclaredThrowableException

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED
import static grails.async.Promises.task

class ProjectService {

    static transactional = false

    static final BRIEF = 'brief'
    static final FLAT = 'flat'
    static final ALL = 'all'
	static final PROMO = 'promo'
    static final OUTPUT_SUMMARY = 'outputs'
    static final ENHANCED = 'enhanced'

    def grailsApplication
    SiteService siteService
    DocumentService documentService
    MetadataService metadataService
    ReportService reportService
    ActivityService activityService
    ProjectActivityService projectActivityService
    PermissionService permissionService
    CollectoryService collectoryService
    WebService webService
    EmailService emailService
    ReportingService reportingService
    OrganisationService organisationService

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    def getBrief(listOfIds, version = null) {
        if (listOfIds) {
            if (version) {
                def all = AuditMessage.findAllByProjectIdInListAndEntityTypeAndDateLessThanEquals(listOfIds, Project.class.name, new Date(version as Long), [sort:'date', order:'desc'])
                def projects = []
                def found = []
                all?.each {
                    if (!found.contains(it.projectId)) {
                        found << it.projectId
                        if (it.entity.status != DELETED &&
                                (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                            projects << [projectId: it.projectId, name: it.entity.name]
                        }
                    }
                }
            } else {
                Project.findAllByProjectIdInListAndStatusNotEqual(listOfIds, DELETED).collect {
                    [projectId: it.projectId, name: it.name]
                }
            }
        } else {
            []
        }
    }

    def get(String id, levelOfDetail = [], version = null) {
        def p = version ?
                AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(id, Project.class.name, new Date(version as Long), [sort:'date', order:'desc', max: 1])[0].entity :
                Project.findByProjectId(id)

        return p?toMap(p, levelOfDetail, version):null
    }

    def list(levelOfDetail = [], includeDeleted = false, citizenScienceOnly = false) {
        def list
        if (!citizenScienceOnly)
            list = includeDeleted ? Project.list(): Project.findAllByStatus(ACTIVE)
        else if (includeDeleted)
            list = Project.findAllByIsCitizenScience(true)
        else
            list = Project.findAllByIsCitizenScienceAndStatus(true, ACTIVE)
        list?.collect { toMap(it, levelOfDetail) }
    }

    def listMeritProjects (levelOfDetail = [], includeDeleted = false){
        def list = []

        if (includeDeleted) {
            list = Project.findAllByIsMERIT(true)
        } else {
            list = Project.findAllByIsMERITAndStatusNotEqual(true, DELETED)
        }
        list.collect { toMap(it, levelOfDetail) }
    }

	def promoted(){
		def list = Project.findAllByPromoteOnHomepage("yes")
		list.collect { toMap(it, PROMO) }
	}

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param prj a Project instance
     * @return map of properties
     */
    Map toMap(project, levelOfDetail = [], includeDeletedActivities = false, version = null) {
        Map result

        Map mapOfProperties = project instanceof Project ? project.getProperty("dbo").toMap() : project

        if (levelOfDetail == BRIEF) {
            result = [
                    projectId           : project.projectId,
                    name                : project.name,
                    grantId             : project.grantId,
                    externalId          : project.externalId,
                    funding             : project.funding,
                    description         : project.description,
                    status              : project.status,
                    plannedStartDate    : project.plannedStartDate,
                    plannedEndDate      : project.plannedEndDate,
                    associatedProgram   : project.associatedProgram,
                    associatedSubProgram: project.associatedSubProgram
            ]
        } else if (levelOfDetail == PROMO) {
            result = [
                    projectId       : project.projectId,
                    name            : project.name,
                    organisationName: project.organisationName,
                    description     : project.description?.take(200),
                    documents       : documentService.findAllForProjectIdAndIsPrimaryProjectImage(project.projectId, ALL)
            ]
        } else {
            String id = mapOfProperties["_id"].toString()
            mapOfProperties["id"] = id
            mapOfProperties["status"] = mapOfProperties["status"]?.capitalize();
            mapOfProperties.remove("_id")

            if (levelOfDetail != FLAT) {
                mapOfProperties.remove("sites")
                mapOfProperties.sites = siteService.findAllForProjectId(project.projectId, [SiteService.FLAT], version)
                mapOfProperties.documents = documentService.findAllForProjectId(project.projectId, levelOfDetail, version)
                mapOfProperties.links = documentService.findAllLinksForProjectId(project.projectId, levelOfDetail, version)

                if (levelOfDetail == ALL) {
                    mapOfProperties.activities = activityService.findAllForProjectId(project.projectId, levelOfDetail, includeDeletedActivities)
                    List<Report> reports = reportingService.findAllForProject(project.projectId)
                    if (reports) {
                        mapOfProperties.reports = reports
                    }
                } else if (levelOfDetail == OUTPUT_SUMMARY) {
                    mapOfProperties.outputSummary = projectMetrics(project.projectId, false, true)
                }
                if (levelOfDetail == ENHANCED) {
                    project.activities = activityService.findAllForProjectId(project.projectId, ActivityService.FLAT, includeDeletedActivities)

                    mapOfProperties.actualStartDate = project.actualStartDate ?: ''
                    mapOfProperties.actualEndDate = project.actualEndDate ?: ''
                    mapOfProperties.plannedDurationInWeeks = project.plannedDurationInWeeks
                    mapOfProperties.actualDurationInWeeks = project.actualDurationInWeeks
                    mapOfProperties.contractDurationInWeeks = project.contractDurationInWeeks
                }
            }

            result = mapOfProperties.findAll { k, v -> v != null }

            // look up current associated organisation details
            result.associatedOrgs?.each {
                if (it.organisationId) {
                    Organisation org = Organisation.findByOrganisationId(it.organisationId)
                    it.name = org.name
                    it.url = org.url
                    it.logo = Document.findByOrganisationIdAndRoleAndStatus(it.organisationId, "logo", ACTIVE)?.thumbnailUrl
                }
            }
        }

        result
    }

    /**
     * Converts the domain object into a highly detailed map of properties, including
     * dynamic properties, and linked components.
     * @param prj a Project instance
     * @return map of properties
     */
    def toRichMap(prj) {
        def dbo = prj.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
		mapOfProperties["status"] = mapOfProperties["status"]?.capitalize();
        mapOfProperties.remove("_id")
        mapOfProperties.remove("sites")
        mapOfProperties.sites = siteService.findAllForProjectId(prj.projectId, true)
        // remove nulls
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props, boolean collectoryLink = true, boolean overrideUpdateDate = false) {
        assert getCommonService()
        try {
            if (props.projectId && Project.findByProjectId(props.projectId)) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Project.withSession { session -> session.clear() }
                return [status:'error',error:'Duplicate project id for create ' + props.projectId]
            }
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            Project project = new Project(projectId: props.projectId?: Identifiers.getNew(true,''), name:props.name)
            // Not flushing on create was causing that further updates to fields were overriden by old values
            project.save(failOnError: true)

            props.remove('sites')
            props.remove('id')


            if(collectoryLink){
                establishCollectoryLinkForProject(project, props)
            }

            getCommonService().updateProperties(project, props, overrideUpdateDate)
            return [status: 'ok', projectId: project.projectId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Project.withSession { session -> session.clear() }
            def error = "Error creating project - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /*
     * Async task for establishing the Collectory data resource - this is because it could be relatively slow and we do
     * not want to delay the project creation process for the user.
     */
    private establishCollectoryLinkForProject(Project project, Map props) {
        task {
            Map collectoryProps = [:]
            collectoryProps << collectoryService.createDataProviderAndResource(project.projectId, props)
            getCommonService().updateProperties(project, collectoryProps)
        }.onComplete {
            log.info("Collectory link established for project ${project.name} (id = ${project.projectId})")
        }.onError { Throwable error ->
            if (error instanceof UndeclaredThrowableException) {
                error = error.undeclaredThrowable
            }
            String message = "Failed to establish collectory link for project ${project.name} (id = ${project.projectId})"
            log.error(message, error)
            emailService.sendEmail(message, "Error: ${error.message}", [grailsApplication.config.ecodata.support.email.address])
        }
    }

    def update(Map props, String id) {
        Project project = Project.findByProjectId(id)
        if (project) {
            try {
                getCommonService().updateProperties(project, props)
                if (project.dataProviderId && project.dataProviderId != "null") {
                    collectoryService.updateDataProviderAndResource(get(id, FLAT))
                } else {
                    establishCollectoryLinkForProject(project, toMap(project, FLAT))
                }
                return [status: 'ok']
            } catch (Exception e) {
                Project.withSession { session -> session.clear() }
                def error = "Error updating project ${id} - ${e.message}"
                log.error error
                return [status: 'error', error: error]
            }
        } else {
            def error = "Error updating project - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /**
     * Deletes a project and any associated activities, outputs and user permissions.  The
     * project is removed from any sites it it associated with.  Orphaned sites are not
     * deleted.
     * @param id the id of the project to delete.
     * @param destroy if false, all deletes will be status updates (a soft delete).  Note that
     * the permissions will be deleted and site associations removed, even in the soft delete case.
     */
    Map delete(String id, boolean destroy) {
        Map result

        Project project = Project.findByProjectId(id)

        if (project) {
            getActivityIdsForProject(id).each {
                activityService.delete(it, destroy)
            }

            projectActivityService.getAllByProject(id).each {
                projectActivityService.delete(it.projectActivityId, destroy)
            }

            permissionService.deleteAllForProject(id, destroy)

            documentService.deleteAllForProject(id, destroy)

            siteService.deleteSitesFromProject(id)

            if (destroy) {
                project.delete(flush: true)
                webService.doDelete(grailsApplication.config.collectory.baseURL + 'ws/dataProvider/' + id)
            } else {
                project.status = DELETED
                project.save(flush: true)
            }

            if (project.hasErrors()) {
                result = [status: 'error', error: project.getErrors()]
            } else {
                result = [status: 'ok']
            }
        } else {
            result = [status: 'error', error: 'No such id']
        }

        result
    }


    /**
     * Returns the reportable metrics for a project as determined by the project output targets and activities
     * that have been undertaken.
     * @param id identifies the project.
     * @return a Map containing the aggregated results.  TODO document me better, but it is likely this structure will change.
     *
     */
    def projectMetrics(String id, targetsOnly = false, approvedOnly = false) {
        def p = Project.findByProjectId(id)
        if (p) {
            def project = toMap(p, ProjectService.FLAT)

            def toAggregate = []

            metadataService.activitiesModel().outputs?.each{
                Score.outputScores(it).each { score ->
                    if (!targetsOnly || score.isOutputTarget) {
                        toAggregate << [score: score]
                    }
                }
            }

            def outputSummary = reportService.projectSummary(id, toAggregate, approvedOnly)

            // Add project output target information where it exists.

            project.outputTargets?.each { target ->
                // Outcome targets are text only and not mapped to a score.
                if (target.outcomeTarget != null) {
                    return
                }
                def result = outputSummary.find{it.label == target.scoreLabel}
                if (result) {
                    if (!result.target || result.target == "0") { // Workaround for multiple outputs inputting into the same score.  Need to update how scores are defined.
                        result.target = target.target
                    }

                } else {
               		   // If there are no Outputs recorded containing the score, the results won't be returned, so add
               			// one in containing the target.
                    def score = toAggregate.find{it.score?.label == target.scoreLabel}
                    if (score) {
                        outputSummary << [label:score.label, score:score.score, target:target.target]
                    } else {
                        // This can happen if the meta-model is changed after targets have already been defined for a project.
                        // Once the project output targets are re-edited and saved, the old targets will be deleted.
                        log.warn "Can't find a score for existing output target: $target.outputLabel $target.scoreLabel, projectId: $project.projectId"
                    }
                }
            }
            return outputSummary
        } else {
            def error = "Error retrieving metrics for project - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

    List<String> getActivityIdsForProject(String projectId) {
        Activity.withCriteria {
            eq("projectId", projectId)
            projections {
                property("activityId")
            }
        }
    }

    /**
     * Performs a case-insensitive search by project name
     * @param name The project name to search for
     * @return List of 'brief' projects with the same name (case-insensitive)
     */
    List<Map> findByName(String name) {
        List<Map> matches = []

        if (name) {
            name = name.replaceAll(" +", " ").trim()
            matches = Project.withCriteria {
                ne "status", DELETED
                rlike "name", "(?i)^${name}\$"
            }.collect { toMap(it, LevelOfDetail.brief) }
        }

        matches
    }

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a list of the projects that match the supplied criteria
     */
    List<Map> search(Map searchCriteria, levelOfDetail = []) {

        def criteria = Project.createCriteria()
        def projects = criteria.list {
            ne("status", DELETED)
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }

        }
        projects.collect{toMap(it, levelOfDetail)}
    }

    /**
     * Updates the organisation name for all projects with the organisation id.
     * (The name is stored alongside the id in the project because not all organisations have entries in the database).
     * @param orgId identifies the organsation that has changed name
     * @param orgName the new organisation name
     */
    void updateOrganisationName(orgId, orgName) {
        Project.findAllByOrganisationIdAndStatusNotEqual(orgId, DELETED).each { project ->
            project.organisationName = orgName
            project.save()
        }
    }

    /**
     * Import SciStarter projects to Biocollect. Import script does the following.
     * 1. gets the list of projects and contacts SciStarter for more details on a project
     * 2. checks if the project is already imported, if yes, update fields. TODO
     * 3. if project does not exist, create a new project, organisation, project extent and project logo document.
     *      And link artifacts to the project. TODO: creating project extent.
     * @return
     */
    Integer importProjectsFromSciStarter(String whiteList){
        List whiteListIds = whiteList?.split (',') ?.collect { Integer.parseInt(it) } ?:[]
        Integer createdProjects = 0

        try {
            String sciStarterProjectUrl

            // delete artifacts from previous import
            List sciStarterSites = Site.findAllByIsSciStarter(true)
            // deleteAll does not fire any messages. Hence elastic search is not updated.
            sciStarterSites.each{ Site site ->
                site.delete()
            }

            List sciStarterProjects = Project.findAllByIsSciStarter(true)
            sciStarterProjects.each{ Project project ->
                project.delete()
            }

            List sciStarterLogo = Document.findAllByIsSciStarter(true)
            sciStarterLogo.each{ Document logo ->
                logo.delete()
            }

            int ignoredProjects = 0

            // list all SciStarter projects
            List projects = getSciStarterProjectsFromFinder()
            projects?.eachWithIndex { pProperties, index ->
                Map transformedProject
                Map project = pProperties
                if (project && project.title) {
                    // get more details about the project
                    sciStarterProjectUrl = "${grailsApplication.config.scistarter.baseUrl}${grailsApplication.config.scistarter.projectUrl}/${project.id}?key=${grailsApplication.config.scistarter.apiKey}"
                    Map projectDetails = webService.getJson(sciStarterProjectUrl)
                    if (!projectDetails.error) {
                        projectDetails << project
                        // switch off the can import project test
                        if (true || SciStarterConverter.canImportProject(projectDetails)) {
                            if (projectDetails.origin && projectDetails.origin == 'atlasoflivingaustralia') {
                                // ignore projects SciStarter imported from Biocollect
                                log.warn("Ignoring ${projectDetails.title} - ${projectDetails.id} - This is an ALA project.")
                                ignoredProjects++
                            } else {
                                // map properties from SciStarter to Biocollect
                                transformedProject = SciStarterConverter.convert(projectDetails)
                                if (projectDetails.id in whiteListIds) {
                                    transformedProject.isWorldWide = false
                                } else {
                                    transformedProject.isWorldWide = true
                                }

                                // create project & document & site & organisation
                                createSciStarterProject (transformedProject, projectDetails)
                                createdProjects ++
                            }
                        } else {
                            log.info("Cannot import project ${projectDetails.title} ${projectDetails.id}")
                            ignoredProjects++
                        }
                    } else {
                        log.error("Ignoring ${project.title} - ${project.id} - since webservice could not lookup details.")
                        ignoredProjects++
                    }
                }

                if(index % 10 == 0 ){
                    log.info( "Imported ${index} scistarter projects")
                    Site.withSession { session -> session.clear()}
                    Project.withSession { session -> session.clear()}
                    Document.withSession { session -> session.clear()}
                    Organisation.withSession { session -> session.clear()}
                }
            }

            log.info("Number of ignored projects ${ignoredProjects}")
        } catch (SocketTimeoutException ste){
            log.error(ste.message, ste)
        } catch (Exception e){
            log.error(e.message, e)
        }

        createdProjects
    }

    /**
     * Get the entire project list from SciStarter
     * @return
     * @throws SocketTimeoutException
     * @throws Exception
     */
    List getSciStarterProjectsFromFinder() throws SocketTimeoutException, Exception{
        String scistarterFinderUrl = "${grailsApplication.config.scistarter.baseUrl}${grailsApplication.config.scistarter.finderUrl}?format=json&q="
        Map response = webService.getJson(scistarterFinderUrl)
        if(response.error){
            if (response.error.contains('Timed out')) {
                throw new SocketTimeoutException(response.error)
            } else {
                throw  new Exception(response.error)
            }
        }

        return response.results
    }

    /**
     * Creates a project in the database. It also creates all associated artifacts like organisation, document, site etc
     * @param transformedProp - mapped SciStarter project properties
     * @return
     */
    Map createSciStarterProject(Map transformedProp, Map rawProp){
        Map organisation

        // create project extent
        Map sites = createSciStarterSites(rawProp)
        String projectSiteId
        if (sites?.siteIds?.size()) {
            projectSiteId = sites.siteIds[0]
        }

        transformedProp.projectSiteId = projectSiteId

        // create organisation
        if (transformedProp.organisationName && transformedProp.organisationName != SciStarterConverter.NO_ORGANISATION_NAME) {
            organisation = createSciStarterOrganisation(transformedProp.organisationName)
            if(organisation.organisationId){
                transformedProp.organisationId = organisation.organisationId
            } else {
                // throw exception?
            }
        }

        // remove unnecessary properties
        String imageUrl = transformedProp.remove('image')
        String attribution = transformedProp.remove('attribution')
        transformedProp.remove('projectId')
        // create project. do not call collectory to create data provider and data resource id
        Map project = create(transformedProp, false, true)
        String projectId = project.projectId

        // use the projectId to associate site with  project
        if (projectId) {
            sites?.siteIds?.each{ siteId ->
                siteService.addProject(siteId, projectId)
            }
            // create project logo.
            createSciStarterLogo(imageUrl, attribution, projectId)

            return project
        } else {
            // todo: reverse transactions
        }
    }

    /**
     * Create sites for a project. if a project has regions then create sites using it.
     * if project does not have region then set extent to the whole world map.
     * @return
     */
    Map createSciStarterSites(Map project){
        Map result = [siteIds:null]
        List sites = []
        if (project.regions?.size()) {
            // convert region to site
            project.regions.each { region ->
                Map site = SciStarterConverter.siteMapping(region)
                // only add valid geojson objects
                if(site?.extent?.geometry && siteService.isGeoJsonValid((site?.extent?.geometry as JSON).toString())){
                    Map createdSite = siteService.create(site)
                    if(createdSite.siteId){
                        sites.push(createdSite.siteId)
                    }
                }
            }

            result.siteIds = sites
        } else {
            // if no region, then create world extent.
            Map world = getWorldExtent()
            if(world.siteId){
                result.siteIds = [world.siteId]
            }
        }

        result
    }

    /**
     * Create project logo. Logo are stored in document collection in Biocollect.
     * @param imageUrl
     * @param attribution
     * @param projectId
     * @return
     */
    Map createSciStarterLogo(String imageUrl, String attribution, String projectId){
        Map props = [
                "externalUrl" : imageUrl,
                "isPrimaryProjectImage" : true,
                "projectId" : projectId,
                "attribution" : attribution,
                "role" : "logo",
                "status" : "active",
                "type" : "link",
                "hasPreview" : false,
                "readOnly" : false,
                "thirdPartyConsentDeclarationRequired" : false,
                "public" : true,
                "stages" : [ ],
                "embeddedVideoVisible" : false,
                "isSciStarter" : true
        ]
        // create logo document
        documentService.create(props, null)
    }

    /**
     * Check organisation.
     * 1. if exist, use it.
     * 2. otherwise, create new
     * @param name
     * @return
     */
    Map createSciStarterOrganisation(String name){
        Organisation org = Organisation.findByName(name)
        if (org) {
            return [organisationId:org.organisationId ]
        } else {
            // create organisation
            Map orgProp = [
                    "collectoryInstitutionId" : "null",
                    "name" : name,
                    "orgType" : "conservation",
                    "description" : "This organisation is imported from SciStarter"
            ]
            return  organisationService.create(orgProp, false)
        }
    }

    /**
     * World extent is used as project area of all SciStarter Projects without project area. This function retrieves
     * site id of world extent already created or creates a world extent.
     * @return - Map - [siteId: 'abc']
     */
    Map getWorldExtent() {
        Map result = siteService.search([name: 'SciStarter Project Area', isSciStarter: true])
        Map worldExtent = [:]
        if (result?.sites?.size()) {
            worldExtent.siteId = result.sites[0].siteId
        } else {
            // use JSON.parse since JSONSlurper converts numbers to BigDecimal which throws error on serialization.
            Object world = JSON.parse(getClass().getResourceAsStream("/data/worldExtent.json")?.getText())
            Map site = siteService.create(world)
            worldExtent.siteId = site.siteId
        }

        return worldExtent
    }
}
