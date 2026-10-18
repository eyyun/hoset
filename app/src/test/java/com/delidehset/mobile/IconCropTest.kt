package com.delidehset.mobile

import com.delidehset.mobile.data.IconStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The crop is what the person sees inside the frame, so the arithmetic has to be the exact
 * inverse of "draw this picture to cover a square, then zoom and pan it". A wrong sign here
 * would put the face in the corner of the icon instead of in the middle.
 */
class IconCropTest {

    private val viewfinder = 400f

    @Test
    fun `a wide picture is cropped from its middle`() {
        val box = IconStore.cropBox(2000, 1000, viewfinder, 1f, 0f, 0f)!!
        assertEquals(500, box.x)
        assertEquals(0, box.y)
        assertEquals(1000, box.width)
        assertEquals(1000, box.height)
    }

    @Test
    fun `a tall picture is cropped from its middle`() {
        val box = IconStore.cropBox(1000, 2000, viewfinder, 1f, 0f, 0f)!!
        assertEquals(0, box.x)
        assertEquals(500, box.y)
        assertEquals(1000, box.width)
    }

    @Test
    fun `panning past the edge stops at the edge`() {
        val box = IconStore.cropBox(2000, 1000, viewfinder, 1f, -5000f, 5000f)!!
        // Fully panned left: the right-hand end of the picture is what the frame shows.
        assertEquals(1000, box.x)
        assertEquals(0, box.y)
        assertEquals(1000, box.width)
    }

    @Test
    fun `zooming in shrinks the source square`() {
        val box = IconStore.cropBox(2000, 1000, viewfinder, 2f, 0f, 0f)!!
        assertEquals(500, box.width)
        assertEquals(750, box.x)
        assertEquals(250, box.y)
    }

    @Test
    fun `a picture smaller than the frame still crops inside it`() {
        val box = IconStore.cropBox(100, 100, viewfinder, 1f, 0f, 0f)!!
        assertEquals(0, box.x)
        assertEquals(0, box.y)
        assertEquals(100, box.width)
    }

    @Test
    fun `nothing to crop is reported as nothing`() {
        assertNull(IconStore.cropBox(0, 100, viewfinder, 1f, 0f, 0f))
        assertNull(IconStore.cropBox(100, 100, 0f, 1f, 0f, 0f))
    }
}
