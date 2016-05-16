package com.xtremelabs.robolectric.shadows;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.pm.Signature;
import android.os.Parcel;

@RunWith(WithTestDefaultsRunner.class)
public class SignatureTest {

    @Test
    public void shouldHaveByteArrayConstructorAndToByteArray() {
        byte[] bytes = { (byte) 0xAC, (byte) 0xDE };
        Signature signature = new Signature(bytes);

        assertArrayEquals(bytes, signature.toByteArray());
    }

    @Test
    public void shouldHaveCreator() throws Exception {
        byte[] bytes = { (byte) 0xAC, (byte) 0xDE };
        Signature expected = new Signature(bytes);
        Parcel p = Parcel.obtain();
        expected.writeToParcel(p, 0);

        p.setDataPosition(0);

        Signature actual = Signature.CREATOR.createFromParcel(p);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldProvideEqualsAndHashCode() throws Exception {
        assertThat(new Signature(new byte[] { (byte) 0xAC }),
                equalTo(new Signature(new byte[] { (byte) 0xAC })));
        assertThat(new Signature(new byte[] { (byte) 0xAC }),
                not(equalTo(new Signature(new byte[] { (byte) 0xDE }))));
        assertThat(new Signature(new byte[] { (byte) 0xAC }).hashCode(),
                equalTo(new Signature(new byte[] { (byte) 0xAC }).hashCode()));
        assertThat(new Signature(new byte[] { (byte) 0xAC }).hashCode(),
                not(equalTo(new Signature(new byte[] { (byte) 0xDE }).hashCode())));
    }
}
