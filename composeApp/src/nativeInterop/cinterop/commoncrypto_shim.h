#include <stddef.h>
#include <stdint.h>

typedef uint32_t CC_LONG;

enum {
  CC_MD5_DIGEST_LENGTH = 16,
  CC_SHA1_DIGEST_LENGTH = 20,
  CC_SHA256_DIGEST_LENGTH = 32,
  CC_SHA512_DIGEST_LENGTH = 64,
};

typedef enum {
  kCCHmacAlgSHA1 = 0,
  kCCHmacAlgMD5 = 1,
  kCCHmacAlgSHA256 = 2,
  kCCHmacAlgSHA384 = 3,
  kCCHmacAlgSHA512 = 4,
} CCHmacAlgorithm;

unsigned char *CC_MD5(const void *data, CC_LONG len, unsigned char *md);
unsigned char *CC_SHA1(const void *data, CC_LONG len, unsigned char *md);
unsigned char *CC_SHA256(const void *data, CC_LONG len, unsigned char *md);
unsigned char *CC_SHA512(const void *data, CC_LONG len, unsigned char *md);

void CCHmac(
  CCHmacAlgorithm algorithm,
  const void *key,
  size_t keyLength,
  const void *data,
  size_t dataLength,
  void *macOut
);
