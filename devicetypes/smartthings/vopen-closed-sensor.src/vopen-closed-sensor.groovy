/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "vOpen/Closed Sensor", namespace: "smartthings", author: "Mark Kreafle") {
		capability "Contact Sensor"
		capability "Sensor"
        
        command "open"
        command "closed"
	}

	// simulator metadata
	simulator {
		// status messages
		status "open":   "open"
		status "closed": "closed"
	}

	// UI tile definitions
	tiles {
		standardTile("contact", "device.contact", width: 2, height: 2) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}

		main "contact"
		details( [ "Open", "Closed" ] )
	}
}

def parse(String description) {
    def name            = parseName(description)
    def value           = parseValue(description)
    def linkText        = getLinkText(device)
    def descriptionText = parseDescriptionText(linkText, value, description)
    def handlerName     = getState(value)
    def isStateChange   = isStateChange(device, name, value)

    def results = [
        translatable:    true,
        name:            name,
        value:           value,
        unit:            null,
        linkText:        linkText,
        descriptionText: descriptionText,
        handlerName:     handlerName,
        isStateChange:   isStateChange,
        displayed:       displayed(description, isStateChange)
    ]
    log.debug "Parse returned $results.descriptionText"

    //def pair = description.split(":")
    //createEvent(name: pair[0].trim(), value: pair[1].trim())

    log.debug "vContact: After createEvent"
    return results
}

private String parseName(String description) {
    if (description?.startsWith("contact: ")) {
        return "contact"
    }
    null
}

private String parseValue(String description) {
    switch(description) {
        case "open": return "open"
        case "closed": return "closed"
        default: return description
    }
}

private parseDescriptionText(String linkText, String value, String description) {
    switch(value) {
        case "open":
            return "{{ linkText }} is open"

        case "closed":
            return "{{ linkText }} is closed"

        default:
            return value
    }
}

private getState(String value) {
    switch(value) {
        case "open":
            return "open"

        case "closed":
            return "closed"

        default:
            return value
    }
}

def open() {
    log.debug "vContact: open"
    sendEvent(name: "contact", value: "open")
}

def closed() {
    log.debug "vContact: closed"

    sendEvent(name: "contact", value: "closed")
}