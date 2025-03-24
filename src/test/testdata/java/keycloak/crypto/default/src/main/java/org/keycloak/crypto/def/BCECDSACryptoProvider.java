package org.keycloak.crypto.def;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.keycloak.common.crypto.ECDSACryptoProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

public class BCECDSACryptoProvider implements ECDSACryptoProvider {


    @Override
    public byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException {
        int len = signLength / 2;
        int arraySize = len + 1;

        byte[] r = new byte[arraySize];
        byte[] s = new byte[arraySize];
        System.arraycopy(signature, 0, r, 1, len);
        System.arraycopy(signature, len, s, 1, len);
        BigInteger rBigInteger = new BigInteger(r);
        BigInteger sBigInteger = new BigInteger(s);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DERSequenceGenerator seqGen = new DERSequenceGenerator(bos);

        seqGen.addObject(new ASN1Integer(rBigInteger.toByteArray()));
        seqGen.addObject(new ASN1Integer(sBigInteger.toByteArray()));
        seqGen.close();
        bos.close();

        return bos.toByteArray();
    }

    @Override
    public byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException {
        int len = signLength / 2;

        ASN1InputStream asn1InputStream = new ASN1InputStream(derEncodedSignatureValue);
        ASN1Primitive asn1Primitive = asn1InputStream.readObject();
        asn1InputStream.close();

        ASN1Sequence asn1Sequence = (ASN1Sequence.getInstance(asn1Primitive));
        ASN1Integer rASN1 = (ASN1Integer) asn1Sequence.getObjectAt(0);
        ASN1Integer sASN1 = (ASN1Integer) asn1Sequence.getObjectAt(1);
        X9IntegerConverter x9IntegerConverter = new X9IntegerConverter();
        byte[] r = x9IntegerConverter.integerToBytes(rASN1.getValue(), len);
        byte[] s = x9IntegerConverter.integerToBytes(sASN1.getValue(), len);

        byte[] concatenatedSignatureValue = new byte[signLength];
        System.arraycopy(r, 0, concatenatedSignatureValue, 0, len);
        System.arraycopy(s, 0, concatenatedSignatureValue, len, len);

        return concatenatedSignatureValue;
    }

    @Override
    public ECPublicKey getPublicFromPrivate(ECPrivateKey ecPrivateKey) {
        try {
            BCECPrivateKey bcecPrivateKey = new BCECPrivateKey(ecPrivateKey, BouncyCastleProvider.CONFIGURATION);

            ECPoint q = bcecPrivateKey.getParameters().getG().multiply(bcecPrivateKey.getD());

            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(q, bcecPrivateKey.getParameters());
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Key algorithm not supported.", e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Received an invalid key spec.", e);
        }
    }

}
