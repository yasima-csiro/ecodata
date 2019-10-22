package au.org.ala.ecodata

import groovyx.net.http.ContentType

class DownloadController {

    def index() { }

    def get(String id) {
        if (!id) {
            response.setStatus(400)
            render "A download ID is required"
        } else {
            String extension = params.fileExtension ?: 'zip'
            File file = new File("${grailsApplication.config.temp.dir}${File.separator}${id}.${extension}")
            if (file.exists()) {
                response.setContentType(ContentType.BINARY.toString())
                response.setHeader('Content-Disposition', 'Attachment;Filename="data.'+extension+'"')

                file.withInputStream { i -> response.outputStream << i }
            } else {
                response.setStatus(404)
                render "No download was found for id: ${id}"
            }
        }
    }
}
