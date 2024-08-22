SUMMARY = "Fast Log processor and Forwarder"
DESCRIPTION = "Fluent Bit is a data collector, processor and  \
forwarder for Linux. It supports several input sources and \
backends (destinations) for your data. \
"

HOMEPAGE = "http://fluentbit.io"
BUGTRACKER = "https://github.com/fluent/fluent-bit/issues"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=2ee41112a44fe7014dce33e26468ba93"
SECTION = "net"

MIN_VER = "${@oe.utils.trim_version("${PV}", 2)}"

SRC_URI = "https://releases.fluentbit.io/${MIN_VER}/source-${PV}.tar.gz;subdir=fluent-bit-${PV};downloadfilename=${BPN}-${PV}.tar.gz \
           file://0002-flb_info.h.in-Do-not-hardcode-compilation-directorie.patch \
           file://0001-monkey-Define-_GNU_SOURCE-for-memmem-API-check.patch \
           file://0006-monkey-Fix-TLS-detection-testcase.patch \
           file://0007-cmake-Do-not-check-for-upstart-on-build-host.patch \
           file://0001-wasm-avoid-cmake-try_run-when-cross-compiling.patch \
           "
SRC_URI:append:libc-musl = "file://0002-chunkio-Link-with-fts-library-with-musl.patch"

SRC_URI[sha256sum] = "3e7b6ca95149db3e7b12f10db651a332ea62ee8038eec0659bee04ca80cac8cf"
S = "${WORKDIR}/fluent-bit-${PV}"

DEPENDS = "zlib bison-native flex-native openssl"
DEPENDS += "${@bb.utils.filter('DISTRO_FEATURES', 'systemd', d)}"

PACKAGECONFIG[yaml] = "-DFLB_CONFIG_YAML=On,-DFLB_CONFIG_YAML=Off,libyaml"
PACKAGECONFIG[kafka] = "-DFLB_OUT_KAFKA=On,-DFLB_OUT_KAFKA=Off,librdkafka"
PACKAGECONFIG[examples] = "-DFLB_EXAMPLES=On,-DFLB_EXAMPLES=Off"
PACKAGECONFIG[jemalloc] = "-DFLB_JEMALLOC=On,-DFLB_JEMALLOC=Off,jemalloc"
#TODO add more fluentbit options to PACKAGECONFIG[]

DEPENDS:append:libc-musl = " fts "

# flex hardcodes the input file in #line directives leading to TMPDIR contamination of debug sources.
do_compile:append() {
    find ${B} -name '*.c' -or -name '*.h' | xargs sed -i -e 's|${TMPDIR}|${TARGET_DBGSRC_DIR}/|g'
}

PACKAGECONFIG ?= "yaml"

LTO = ""

# Use CMake 'Unix Makefiles' generator
OECMAKE_GENERATOR ?= "Unix Makefiles"

# Fluent Bit build options
# ========================

# Host related setup
EXTRA_OECMAKE += "-DGNU_HOST=${HOST_SYS} -DFLB_TD=1"

# Disable LuaJIT and filter_lua support
EXTRA_OECMAKE += "-DFLB_LUAJIT=Off -DFLB_FILTER_LUA=Off "

# Disable Library and examples
EXTRA_OECMAKE += "-DFLB_SHARED_LIB=Off"

# Enable systemd iff systemd is in DISTRO_FEATURES
EXTRA_OECMAKE += "${@bb.utils.contains('DISTRO_FEATURES','systemd','-DFLB_SYSTEMD=On','-DFLB_SYSTEMD=Off',d)}"

# Enable release builds
EXTRA_OECMAKE += "-DFLB_RELEASE=On"

# musl needs these options
EXTRA_OECMAKE:append:libc-musl = ' -DFLB_JEMALLOC_OPTIONS="--with-jemalloc-prefix=je_ --with-lg-quantum=3" -DFLB_CORO_STACK_SIZE=24576'

EXTRA_OECMAKE:append:riscv64 = " -DCMAKE_C_STANDARD_LIBRARIES=-latomic"
EXTRA_OECMAKE:append:riscv32 = " -DCMAKE_C_STANDARD_LIBRARIES=-latomic"
EXTRA_OECMAKE:append:mips = " -DCMAKE_C_STANDARD_LIBRARIES=-latomic"
EXTRA_OECMAKE:append:powerpc = " -DCMAKE_C_STANDARD_LIBRARIES=-latomic"
EXTRA_OECMAKE:append:x86 = " -DCMAKE_C_STANDARD_LIBRARIES=-latomic"

inherit cmake systemd pkgconfig

SYSTEMD_SERVICE:${PN} = "fluent-bit.service"

EXTRA_OECMAKE += "-DCMAKE_DEBUG_SRCDIR=${TARGET_DBGSRC_DIR}/"
TARGET_CC_ARCH += " ${SELECTED_OPTIMIZATION}"

SKIP_RECIPE[fluentbit] ?= "It is not reproducible. QA Issue: File /usr/bin/.debug/td-agent-bit in package fluentbit-dbg contains reference to TMPDIR [buildpaths]"
