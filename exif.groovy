#!/usr/bin/env groovy 

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

def file = new File(args[0])

//        get all metadata stored in EXIF format (ie. from JPEG or TIFF).
//            org.w3c.dom.Node node = Sanselan.getMetadataObsolete(imageBytes);
IImageMetadata metadata = Sanselan.getMetadata(file)
println("file: ${file.getPath()}")

if (!metadata) {
    println("\tNo EXIF metadata was found")
}

if (metadata instanceof JpegImageMetadata) {
    JpegImageMetadata jpegMetadata = metadata

    // Jpeg EXIF metadata is stored in a TIFF-based directory structure
    // and is identified with TIFF tags.
    // Here we look for the "x resolution" tag, but
    // we could just as easily search for any other tag.
    //
    // see the TiffConstants file for a list of TIFF tags.

    

    // print out various interesting EXIF tags.
    println("  -- Standard EXIF Tags")
    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION)
    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE)
    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE_REF)
    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE)
    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF)
    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE)
    


    // simple interface to GPS data
    println("  -- GPS Info")
    TiffImageMetadata exifMetadata = jpegMetadata.exif
    if (exifMetadata) {
        TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS()
        if (gpsInfo) {
            double longitude = gpsInfo.longitudeAsDegreesEast
            double latitude = gpsInfo.latitudeAsDegreesNorth

            println("        GPS Description: ${gpsInfo}")
            println("        GPS Longitude (Degrees East): ${longitude}")
            println("        GPS Latitude (Degrees North): ${latitude}")
        }
    }

    println("  -- All EXIF info")
    ArrayList items = jpegMetadata.items.each { item ->
       println("        ${item}")   
    }
    println("")
}

void printTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
    TiffField field = jpegMetadata.findEXIFValue(tagInfo);
    if (field == null) {
        println("        (" + tagInfo.name + " not found.)")
    }
    else {
        println("        " + tagInfo.name + ": " + field.valueDescription)
    }
}