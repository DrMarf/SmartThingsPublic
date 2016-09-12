/**
 *  XfinityTVControl
 *
 *  Copyright 2016 Mark Kreafle
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
definition(
    name: "XfinityTVControl",
    namespace: "theshoctor",
    author: "Mark Kreafle",
    description: "I want to be able to control the TVs.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") 


preferences {
	section ("xFinity Logon") {
        input name: "xUsername", type: "text", title: "xFinity Username", description: "Enter Username", required: true
        input name: "xPassword", type: "password", title: "xFinity Password", description: "Enter Password", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    updateAuthentication()
}

// TODO: implement event handlers

def updateAuthentication() {
	def LOGIN_URL = "https://login.comcast.net/"
    def API_PREFIX = "http://xfinitytv.comcast.net"
    def PROFILE_API = API_PREFIX + "/xtv/authkey/user"
    def TOKEN_API = API_PREFIX + "/xip/fc-rproxy/rtune/authtoken"
    def CHANNEL_API = API_PREFIX + "/xip/fc-rproxy/rtune/device/%s/tune/tv/vcn/%s"
    def LISTING_API = API_PREFIX + "/xfinityapi/channel/lineup/headend/%s/"

    def cookie = ""
	state.xf_s_ticket = ""
    
	log.debug "Starting Request..."
    def params = [
        uri:  LOGIN_URL,
        path: 'login'
    ]
    try {
        httpGet(params) {resp ->
        	//log.debug "Response 1 Headers: ${resp.headers}"
        	//log.debug "Response 1 Data   : ${resp.data}"
            resp.headers.each {
            	//log.debug "Response 1 Header : ${it.name} : ${it.value}"
          	}
            cookie = resp.getHeaders('Set-Cookie')
            
            //Build the next request. Nesting because of weird ASYNC issues...
			//log.debug "Authenticating..."
    
    		def postBody = ['user' : settings.xUsername,
                'passwd' : settings.xPassword ]
    		def paramsp = [
	    		uri : "$LOGIN_URL/login",
        		contentType: "application/x-www-form-urlencoded",
        		body : postBody,
        		headers: ["Cookie" : cookie]
    		]
    		//log.debug "Auth Params -> $paramsp"
			httpPost(paramsp) { response ->
          		//log.debug "Response 2 Received: Status [$response.status]"
          		response.headers.each {
            		//log.debug "Response 2 Header : ${it.name} : ${it.value}"
            		if (it.name == "Set-Cookie") {
                        if(it.value.startsWith("s_ticket=")){
                        	//log.debug "s_ticket : ${it.value}"
                            state.xf_s_ticket = it.value
                        }
            		}
          		}
                if (state.xf_s_ticket.size() > 10) {
    				//log.debug "Logon Part 1 Successful"
    	           	//log.debug state.xf_s_ticket
    			} else {
    				//log.debug "Logon Part 1 Failure"
                	return false
    			}
                    
                def params3 = [
	    			uri : PROFILE_API,
        			headers: ["Cookie" : state.xf_s_ticket],
                    query: ['p' : state.xf_s_ticket.minus("s_ticket=")]
    			]
                log.debug "Params3 -> $params3"
                httpGet(params3) {response3 ->
                	//log.debug "Response 3 Received: Status [$response3.status]"
        			response3.headers.each {
            			//log.debug "Response 3 Header : ${it.name} : ${it.value}"
          			}
                    
          			log.debug "Response 3 Data  : ${response3.data}"
                    
            	    if (state.xf_s_ticket.size() > 10) {
    					log.debug "Logon Part 3 Successful"
    	            	log.debug state.xf_s_ticket
	                	return true
    				} else {
    					log.debug "Logon Part 3 Failure"
                		return false
    				}
                }
    		}
        }
    } catch (e) {
        log.error "error: $e"
        return false
    }
    
    
    
}