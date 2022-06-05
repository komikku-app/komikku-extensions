from base64 import b64encode

with open('banner.jpg', 'rb') as f:
    data = f.read()

head = b'''\
package eu.kanade.tachiyomi.extension.zh.baozimanhua

const val BANNER_BASE64 = "\
'''

tail = b'''\
"
const val BANNER_WIDTH = 800
const val BANNER_HEIGHT = 282
'''

with open('src/eu/kanade/tachiyomi/extension/zh/baozimanhua/BannerData.kt', 'wb') as f:
    f.write(head)
    f.write(b64encode(data))
    f.write(tail)
