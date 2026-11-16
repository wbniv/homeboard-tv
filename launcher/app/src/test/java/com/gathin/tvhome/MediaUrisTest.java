package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class MediaUrisTest {
    @Test public void upgradesHttpCdnUrlsWithoutChangingEscapedPathsOrQueries() {
        assertEquals("https://cdn.example.com/Aerials/a%2Fb.mov?token=a%2Bb#frame",
                MediaUris.secure("http://cdn.example.com:80/Aerials/a%2Fb.mov?token=a%2Bb#frame"));
        assertEquals("https://example.com:8443/test", MediaUris.secure("http://example.com:8443/test"));
    }
    @Test public void plaintextIsLimitedToPublishedAppleMediaRoutesWithoutCredentials() {
        assertTrue(MediaUris.appleHttp("http://sylvan.apple.com/Aerials/video.mov"));
        assertTrue(MediaUris.appleHttp("http://a1.phobos.apple.com/us/r1000/video.m4v"));
        assertFalse(MediaUris.appleHttp("http://sylvan.apple.com.evil.example/Aerials/video.mov"));
        assertFalse(MediaUris.appleHttp("http://user:secret@sylvan.apple.com/Aerials/video.mov"));
        assertFalse(MediaUris.appleHttp("http://sylvan.apple.com:8080/Aerials/video.mov"));
        assertFalse(MediaUris.appleHttp("http://sylvan.apple.com/private"));
        assertEquals("http://sylvan.apple.com/Aerials/video.mov", MediaUris.secure("http://sylvan.apple.com/Aerials/video.mov"));
    }
    @Test public void preservesTlsAndAccessibleAndroidUris() {
        assertEquals("https://example.com/image", MediaUris.secure("HTTPS://example.com/image"));
        for (String uri : new String[]{"https://example.com/a%20b", "content://example/image/1", "android.resource://example/raw/image"})
            assertEquals(uri, MediaUris.secure(uri));
    }
    @Test public void rejectsPrivateFileAndScriptSourcesAndMalformedHosts() {
        for (String uri : new String[]{"file:///data/private", "javascript:alert(1)", "data:abc", "http:///missing-host"}) {
            try { MediaUris.secure(uri); fail(uri); } catch (IllegalArgumentException expected) { }
        }
    }
}
