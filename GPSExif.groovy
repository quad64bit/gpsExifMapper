@Grab(group='org.apache.sanselan', module='sanselan', version='0.97-incubator')
import org.apache.sanselan.ImageReadException
import org.apache.sanselan.Sanselan
import org.apache.sanselan.common.IImageMetadata
import org.apache.sanselan.common.RationalNumber
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.tiff.TiffField
import org.apache.sanselan.formats.tiff.TiffImageMetadata
import org.apache.sanselan.formats.tiff.constants.TagInfo
import org.apache.sanselan.formats.tiff.constants.TiffConstants

import groovy.json.JsonSlurper

File loadSource, sortStore
if(args.size() < 2){
    println "You need to provide a path to load the images, and a path to store the sorted images"
} else{
    loadSource = new File(args[0])
    sortStore = new File("${args[1]}/SortedImages")
    sortStore.mkdirs()
}

void makeMap(double lat, double lng, File htmlFile){
    String htmlCode = """
<html>
    <head>
        <title>Map for ${lat},${lng}</title>
        <script type=\"text/javascript\" src=\"http://maps.googleapis.com/maps/api/js?sensor=false\"></script>
        <script type=\"text/javascript\">
            var geocoder;
            var map;

            function initialize() {
                var latlng = new google.maps.LatLng(${lat}, ${lng});
                geocoder = new google.maps.Geocoder();
                var myOptions = {
                    zoom: 20,
                    center: latlng,
                    mapTypeId: google.maps.MapTypeId.HYBRID
                };
                map = new google.maps.Map(document.getElementById(\"map_canvas\"), myOptions);

                var btard = new google.maps.Marker({
                    position:latlng,
                    map:map,
                    title:\"B-Tard\",
                    animation: google.maps.Animation.DROP
                })
            }
        </script>
    </head>
    <body onload=\"initialize()\">
        <div id=\"map_canvas\" style=\"width:100%; height:80%\"></div>
    </body>
</html>
"""

    htmlFile.write(htmlCode)
}

def ant = new AntBuilder()
def count = 1
loadSource.traverse{ file ->
    if(!file.isDirectory()){
        // println "looking at: ${file}"
        IImageMetadata metadata
        try{
            metadata = Sanselan.getMetadata(file)
        }catch(Exception e){
           // "No exif or bad format"
          //  println "bad exif: ${file}"
        }

        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegMetadata = metadata
            TiffImageMetadata exifMetadata = jpegMetadata.exif

            if (exifMetadata) {
                TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS()
                if (gpsInfo) {
                    println "GPS Data Found"
                    double latitude = gpsInfo.latitudeAsDegreesNorth
                    double longitude = gpsInfo.longitudeAsDegreesEast
/*                    println("${longitude}")
                    println("${latitude}")*/
                    def jsonResponse = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=${latitude},${longitude}&sensor=false").getText()
                    def jsonObj = new JsonSlurper().parseText(jsonResponse)

                    def comp = jsonObj.results
                    println comp.formatted_address[0]

                    if(comp.formatted_address[0]){
                        comp.address_components[0].each{
                            if(it.types[0] == "administrative_area_level_1"){
                                if(it.long_name){
                                    def f = new File("${sortStore}/${it.long_name}")
                                    ant.copy(file:file, tofile:"${f}/${comp.formatted_address[0]} - ${file.name}")
                                    new File("${f}/${comp.formatted_address[0]} - ${file.name}.txt").write("${latitude},${longitude}\n----------------------------------------------------\n${jpegMetadata}\n----------------------------------------------------\n${exifMetadata}")
                                    makeMap(latitude, longitude, new File("${f}/${comp.formatted_address[0]} - ${file.name}.html"))
                                }
                            }
                        }
                    } else{
                        def f = new File("${sortStore}/NoAddress")
                        f.mkdirs()
                        ant.copy(file:file, tofile:"${f}/${file.name}")
                        new File("${f}/${file.name}.txt").write("${latitude},${longitude}\n----------------------------------------------------\n${jpegMetadata}\n----------------------------------------------------\n${exifMetadata}")
                    }
                } else{
                  println "no gps meta: ${file}"
                }
            } else{
              // println "no exif: ${file}"
            }
        }
    }
}

null
