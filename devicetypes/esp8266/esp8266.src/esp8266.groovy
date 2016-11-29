/**
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
 *	ESP8266 Cloud Controller
 *
 *	Author: Mark Kreafle
 *	Date: 2016-03-16
 */
 preferences {
    input("ip", "string", title:"IP Address", description: "10.0.0.X", required: true, displayDuringSetup: true)
    input("port", "string", title:"Port", description: "80", defaultValue: 80 , required: true, displayDuringSetup: true)
	input("mac", "text", title: "MAC Addr", description: "mac")
}

 metadata {
	definition (name: "esp8266", namespace: "esp8266", author: "mkreafle") {
		capability "Switch Level"
		capability "Actuator"
		capability "Indicator"
		capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
	}

	// simulator metadata
	simulator {
    	
    }

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "On", label:'${name}', action:"switch.off", icon:"st.alarm.water.wet", backgroundColor:"#7475c3", nextState:"turningOff"
				attributeState "Off", label:'${name}', action:"switch.on", icon:"st.alarm.water.dry", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.alarm.water.wet", backgroundColor:"#7475c3", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.alarm.water.dry", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
        
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}
        
        standardTile("indicator", "device.indicatorStatus", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
			state "On", action:"indicator.indicatorWhenOn", icon:"st.custom.sonos.unmuted"
			state "Off", action:"indicator.indicatorWhenOff", icon:"st.custom.sonos.muted"
		}
        
        valueTile("temperature", "device.temperature", inactiveLabel: false) {
			state "temperature", label:'${currentValue}°${unit}', unit:"F",
			backgroundColors:[
				[value: 31, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}
        
        standardTile("refresh", "device.backdoor", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
		main "switch"
		details (["switch", "level", "indicator"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
	def headerString = msg.header

	if (!headerString) {
		log.debug "headerstring was null for some reason :("
    }

	def result = []
	def bodyString = msg.body
    def value = "";
	if (bodyString) {
        log.debug bodyString
        // default the contact and motion status to closed and inactive by default
        def allContactStatus = "closed"
        def allMotionStatus = "inactive"
        def json = msg.json;
        json?.house?.door?.each { door ->
            value = door?.status == 1 ? "open" : "closed"
            log.debug "${door.name} door status ${value}"
            // if any door is open, set contact to open
            if (value == "open") {
				allContactStatus = "open"
			}
			result << creatEvent(name: "temperature", value: allContactStatus)
        }


		
		//result << createEvent(name: "motion", value: allMotionStatus)
    }
    result
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

def refresh() {
	
    if(device.deviceNetworkId!=settings.mac) {
    	log.debug "setting device network id"
    	device.deviceNetworkId = settings.mac;
    }
	log.debug "REFESHING BY: Executing Arduino 'poll'" 
    sendEvent(name:  "temperature", value: "69", unit:  "F")
    poll()
}

def poll() {
	log.debug "Executing 'poll' ${getHostAddress()} and send data to ${getCallBackAddress()}"
	setDeviceNetworkId(ip,port)
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP")]
	)
    
	sendEvent(name:  "temperature", value: "66", unit:  "F")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	//device.deviceNetworkId = "$iphex:$porthex"
  	//log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

def on() {
	log.debug "Turning On"
	// Remember to remove the fake events!
    sendEvent(name: "switch", value: "On")
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/on",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP")]
	)
}

def off() {
	log.debug "Turning Off"
	sendEvent(name: "switch", value: "Off")
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/off",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP")]
	)
}

def indicatorWhenOff() {
	log.debug "Turning Indicator On"
	sendEvent(name: "indicatorStatus", value: "On")
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/soundon",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP")]
	)
}

def indicatorWhenOn() {
	log.debug "Turning Indicator Off"
	sendEvent(name: "indicatorStatus", value: "Off")
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/soundoff",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP")]
	)
}

def setLevel(value) {
	log.debug "setLevel >> value: $value"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	if (level > 0) {
		sendEvent(name: "switch", value: "on")
	} else {
		sendEvent(name: "switch", value: "off")
	}
	sendEvent(name: "level", value: level, unit: "%")
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/level",
    	headers: [
        	HOST: "${getHostAddress()}"
    	],
        query:[hubip: device.hub.getDataValue("localIP"), hubport: device.hub.getDataValue("localSrvPortTCP"), level: level]
	)
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000
}