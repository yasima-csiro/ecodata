class UrlMappings {

	static mappings = {

        "/ws/activitiesForProject/$id" {
            controller = 'activity'
            action = 'activitiesForProject'
        }

        "/ws/assessment/$id?" {
            controller = 'activity'
            type = 'assessment'
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/$controller/$id?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/metadata/$action/$id?" {
            controller = 'metadata'
        }

        "/ws/$entity/$id/raw" {
            controller = 'admin'
            action = 'getBare'
        }

		"/ws/$controller/$action?/$id?" {
		}

		"/$controller/$action?/$id?" {
		}

        "/ws/documentation/$version/$action/$id?" {
            controller = 'documentation'
        }

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
