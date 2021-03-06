package com.horizen.box;

import com.horizen.proposition.PublicKey25519Proposition;
import org.junit.Before;
import org.junit.Test;
import scala.Tuple2;
import scorex.crypto.signatures.Curve25519;

import java.util.Arrays;

import static org.junit.Assert.*;

public class CertifierRightBoxTest
{
    private PublicKey25519Proposition proposition;
    private long nonce;
    private long value;
    private long minimumWithdrawalEpoch;

    @Before
    public void setUp() {
        byte[] anotherSeed = "certrighttestseed".getBytes();
        Tuple2<byte[], byte[]> keyPair = Curve25519.createKeyPair(anotherSeed);
        proposition = new PublicKey25519Proposition(keyPair._2());

        nonce = 12345;
        value = 1;
        minimumWithdrawalEpoch = 5;
    }

    @Test
    public void creation() {
        CertifierRightBox box = new CertifierRightBox(proposition, nonce, value, minimumWithdrawalEpoch);

        assertEquals("CertifierRightBox creation: proposition is wrong", proposition, box.proposition());
        assertEquals("CertifierRightBox creation: nonce is wrong", nonce, box.nonce());
        assertEquals("CertifierRightBox creation: activeFromWithdrawalEpoch is wrong", minimumWithdrawalEpoch, box.activeFromWithdrawalEpoch());
        assertEquals("CertifierRightBox creation: value is wrong", value, box.value());
    }

    @Test
    public void comparison() {
        CertifierRightBox box1 = new CertifierRightBox(proposition, nonce, value, minimumWithdrawalEpoch);
        CertifierRightBox box2 = new CertifierRightBox(proposition, nonce, value, minimumWithdrawalEpoch);

        assertEquals("Boxes hash codes expected to be equal", box1.hashCode(), box2.hashCode());
        assertEquals("Boxes expected to be equal", box1, box2);
        assertTrue("Boxes ids expected to be equal", Arrays.equals(box1.id(), box2.id()));


        byte[] anotherSeed = "another test seed".getBytes();
        Tuple2<byte[], byte[]> keyPair = Curve25519.createKeyPair(anotherSeed);
        PublicKey25519Proposition anotherProposition = new PublicKey25519Proposition(keyPair._2());


        CertifierRightBox box3 = new CertifierRightBox(anotherProposition, nonce, value, minimumWithdrawalEpoch);
        assertNotEquals("Boxes hash codes expected to be different", box1.hashCode(), box3.hashCode());
        assertNotEquals("Boxes expected to be different", box1, box3);
        assertFalse("Boxes ids expected to be different", Arrays.equals(box1.id(), box3.id()));


        CertifierRightBox box4 = new CertifierRightBox(proposition, nonce + 1, value, minimumWithdrawalEpoch);
        assertEquals("Boxes hash codes expected to be equal", box1.hashCode(), box4.hashCode());
        assertNotEquals("Boxes expected to be different", box1, box4);
        assertFalse("Boxes ids expected to be different", Arrays.equals(box1.id(), box4.id()));


        CertifierRightBox box5 = new CertifierRightBox(proposition, nonce, value, minimumWithdrawalEpoch + 1);
        assertEquals("Boxes hash codes expected to be equal", box1.hashCode(), box5.hashCode());
        assertNotEquals("Boxes expected to be different", box1, box5);
        assertTrue("Boxes ids expected to be different", Arrays.equals(box1.id(), box5.id()));

        CertifierRightBox box6 = new CertifierRightBox(proposition, nonce, value + 1, minimumWithdrawalEpoch);
        assertEquals("Boxes hash codes expected to be equal", box1.hashCode(), box6.hashCode());
        assertNotEquals("Boxes expected to be different", box1, box6);
        assertTrue("Boxes ids expected to be different", Arrays.equals(box1.id(), box6.id()));
    }
}