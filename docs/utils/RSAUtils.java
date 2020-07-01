import javax.crypto.Cipher;
import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class RSAUtils {
    private static int reserveSize = 11;
    private static String cipherAlgorithm = "RSA/ECB/PKCS1Padding";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    public static byte[] decrypt(byte[] encryptedBytes, RSAPrivateKey privateKey) throws Exception {
        int keyByteSize = privateKey.getModulus().bitLength() / 8;
        int decryptBlockSize = keyByteSize - reserveSize;
        int nBlock = encryptedBytes.length / keyByteSize;
        try (ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * decryptBlockSize)){
            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            for (int offset = 0; offset < encryptedBytes.length; offset += keyByteSize) {
                int inputLen = encryptedBytes.length - offset;
                if (inputLen > keyByteSize) {
                    inputLen = keyByteSize;
                }
                byte[] decryptedBlock = cipher.doFinal(encryptedBytes, offset, inputLen);
                outbuf.write(decryptedBlock);
            }
            outbuf.flush();
            return outbuf.toByteArray();
        } catch (Exception e) {
            throw new Exception("DEENCRYPT ERROR:", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedBytes, String RSAPrivateKey) throws Exception {
        RSAPrivateKey privateKey = parsePrivateKey(RSAPrivateKey);
        return decrypt(encryptedBytes, privateKey);
    }

    public static byte[] encrypt(byte[] plainBytes, RSAPublicKey publicKey) throws Exception {
        int keyByteSize = publicKey.getModulus().bitLength() / 8;
        int encryptBlockSize = keyByteSize - reserveSize;
        int nBlock = plainBytes.length / encryptBlockSize;
        if ((plainBytes.length % encryptBlockSize) != 0) {
            nBlock += 1;
        }
        try (ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * keyByteSize)){
            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            for (int offset = 0; offset < plainBytes.length; offset += encryptBlockSize) {
                int inputLen = plainBytes.length - offset;
                if (inputLen > encryptBlockSize) {
                    inputLen = encryptBlockSize;
                }
                byte[] encryptedBlock = cipher.doFinal(plainBytes, offset, inputLen);
                outbuf.write(encryptedBlock);
            }
            outbuf.flush();
            return outbuf.toByteArray();
        } catch (Exception e) {
            throw new Exception("ENCRYPT ERROR:", e);
        }
    }

    public static byte[] encrypt(byte[] plainBytes, String rasPublicKey) throws Exception {
        RSAPublicKey publicKey = parsePublicKey(rasPublicKey);
        return encrypt(plainBytes, publicKey);
    }

    /**
     * 用私钥加密信息，生成数字签名
     *
     * @param data       需要加密的数据
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, RSAPrivateKey privateKey) throws Exception {
        // 用私钥对信息生成数字签名
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return encodeBase64(signature.sign());
    }

    public static String sign(byte[] data, String privateKey) throws Exception {
        RSAPrivateKey privateKey1 = parsePrivateKey(privateKey);
        return sign(data, privateKey1);
    }

    /**
     * 使用公钥解密，校验数字签名
     *
     * @param data      解密后的数据
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return 校验成功返回true 失败返回false
     * @throws Exception
     */
    public static boolean verify(byte[] data, RSAPublicKey publicKey, String sign)
            throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        // 验证签名是否正常
        return signature.verify(decodeBase64(sign));
    }

    public static RSAPrivateKey getPriKey(String privateKeyPath){
        RSAPrivateKey privateKey = null;
        try (InputStream inputStream = new FileInputStream(privateKeyPath)){
            privateKey = getPrivateKey(inputStream);
        } catch (Exception e) {
            System.out.println("加载私钥出错!");
        }
        return privateKey;
    }

    public static RSAPrivateKey parsePrivateKey(String encodeKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(decodeBase64(encodeKey));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(priPKCS8);
    }

    public static RSAPrivateKey getPrivateKey(InputStream inputStream) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))){
            StringBuilder sb = new StringBuilder();
            String readLine = null;
            while ((readLine = br.readLine()) != null) {
                if (readLine.charAt(0) == '-') {
                    continue;
                } else {
                    sb.append(readLine);
                }
            }
            return parsePrivateKey(sb.toString());
        } catch (Exception e) {
            throw new Exception("READ PRIVATE KEY ERROR:" ,e);
        }
    }
    public static RSAPublicKey getPubKey(String publicKeyPath){
        RSAPublicKey publicKey = null;
        try (InputStream inputStream = new FileInputStream(publicKeyPath)){
            publicKey = getPublicKey(inputStream);
        } catch (Exception e) {
            System.out.println("加载公钥出错!");
        }
        return publicKey;
    }

    public static RSAPublicKey parsePublicKey(String publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec pubX509 = new X509EncodedKeySpec(decodeBase64(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return (RSAPublicKey)keyFactory.generatePublic(pubX509);
    }

    public static RSAPublicKey getPublicKey(InputStream inputStream) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))){
            StringBuilder sb = new StringBuilder();
            String readLine = null;
            while ((readLine = br.readLine()) != null) {
                if (readLine.charAt(0) == '-') {
                    continue;
                } else {
                    sb.append(readLine);
                }
            }
            return parsePublicKey(sb.toString());
        } catch (Exception e) {
            throw new Exception("READ PUBLIC KEY ERROR:", e);
        }
    }
    public static byte[] decodeBase64(String key) {
        return Base64.getDecoder().decode(key);
    }
    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 把Key保存到文件
     * @param path 文件路径
     * @param key 秘钥
     * @throws IOException
     */
    public static void save(String path, Key key) throws IOException {
        try(FileOutputStream out = new FileOutputStream(new File(path))){
            out.write(encodeBase64(key.getEncoded()).getBytes());
        }
    }

    /**
     * 初始化密钥
     *
     * @return
     * @throws Exception
     */
    public static KeyPair initKeyPair(int keySize) throws Exception {
        SecureRandom random = new SecureRandom();
        KeyPairGenerator keyPairGen = KeyPairGenerator
                .getInstance(KEY_ALGORITHM, new org.bouncycastle.jce.provider.BouncyCastleProvider());
        keyPairGen.initialize(keySize, random);
        return keyPairGen.generateKeyPair();
    }
}