package org.synthesis.infrastructure

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

inline fun ZipOutputStream.addFile(
    fileName: String,
    addedAttachments: MutableMap<String, Int>,
    code: (ZipOutputStream.() -> Unit)
) {
    /**
     * If files with the same name as the existing one are added to the archive, the existing file will be
     * overwritten.
     * To correctly add duplicate names, we will rename the file, adding an index to the beginning
     */
    var currentIndex = addedAttachments[fileName]

    if (currentIndex == null) {
        currentIndex = 1
        addedAttachments[fileName] = currentIndex
    } else {
        currentIndex++
        addedAttachments[fileName] = currentIndex
    }

    putNextEntry(ZipEntry("${currentIndex}_$fileName"))

    code()

    closeEntry()
}
