package scala.scalanative
package build

import java.nio.file.{Files, Path, Paths}

/** An object describing how to configure the Scala Native toolchain. */
sealed trait Config {

  private val testSuffix = "-test"

  /** Base Directory for native work products. */
  def basedir: Path

  /** Indicates whether this is a test config or not. */
  def testConfig: Boolean

  /** Directory to emit intermediate compilation results. Calculated based on
   *  [[basedir]] / native or native-test if a test project. The build creates
   *  directories if they do not exist.
   */
  def workdir: Path

  /** Path to the output file, executable or library. Calculated based on
   *  [[basedir]] / [[NativeConfig#basename]] and -test, if a test project.
   */
  def artifactPath: Path

  /** Entry point for linking. */
  def mainClass: String

  /** Sequence of all NIR locations. */
  def classPath: Seq[Path]

  /** The logger used by the toolchain. */
  def logger: Logger

  def compilerConfig: NativeConfig

  /** Create a new config with given base directory. */
  def withBasedir(value: Path): Config

  /** Create a new config with test (true) or normal config (false). */
  def withTestConfig(value: Boolean): Config

  /** Create new config with given mainClass point. */
  def withMainClass(value: String): Config

  /** Create a new config with given nir paths. */
  def withClassPath(value: Seq[Path]): Config

  /** Create a new config with the given logger. */
  def withLogger(value: Logger): Config

  def withCompilerConfig(value: NativeConfig): Config

  def withCompilerConfig(fn: NativeConfig => NativeConfig): Config

  /** The garbage collector to use. */
  def gc: GC = compilerConfig.gc

  /** Compilation mode. */
  def mode: Mode = compilerConfig.mode

  /** The path to the `clang` executable. */
  def clang: Path = compilerConfig.clang

  /** The path to the `clang++` executable. */
  def clangPP: Path = compilerConfig.clangPP

  /** The options passed to LLVM's linker. */
  def linkingOptions: Seq[String] = compilerConfig.linkingOptions

  /** The compilation options passed to LLVM. */
  def compileOptions: Seq[String] = compilerConfig.compileOptions

  /** Should stubs be linked? */
  def linkStubs: Boolean = compilerConfig.linkStubs

  /** The LTO mode to use used during a release build. */
  def LTO: LTO = compilerConfig.lto

  /** Shall linker check that NIR is well-formed after every phase? */
  def check: Boolean = compilerConfig.check

  /** Shall linker dump intermediate NIR after every phase? */
  def dump: Boolean = compilerConfig.dump

  protected def nameSuffix = if (testConfig) testSuffix else ""

  private[scalanative] def targetsWindows: Boolean = {
    compilerConfig.targetTriple.fold(Platform.isWindows) { customTriple =>
      customTriple.contains("win32") ||
      customTriple.contains("windows")
    }
  }
}

object Config {

  /** Default empty config object where all of the fields are left blank. */
  def empty: Config =
    Impl(
      nativelib = Paths.get(""),
      mainClass = "",
      classPath = Seq.empty,
      basedir = Paths.get(""),
      testConfig = false,
      logger = Logger.default,
      compilerConfig = NativeConfig.empty
    )

  private final case class Impl(
      nativelib: Path,
      mainClass: String,
      classPath: Seq[Path],
      basedir: Path,
      testConfig: Boolean,
      logger: Logger,
      compilerConfig: NativeConfig
  ) extends Config {
    def withNativelib(value: Path): Config =
      copy(nativelib = value)

    def withMainClass(value: String): Config =
      copy(mainClass = value)

    def withClassPath(value: Seq[Path]): Config =
      copy(classPath = value)

    def withBasedir(value: Path): Config =
      copy(basedir = value)

    def withTestConfig(value: Boolean): Config =
      copy(testConfig = value)

    def withLogger(value: Logger): Config =
      copy(logger = value)

    override def withCompilerConfig(value: NativeConfig): Config =
      copy(compilerConfig = value)

    override def withCompilerConfig(fn: NativeConfig => NativeConfig): Config =
      copy(compilerConfig = fn(compilerConfig))

    override def workdir: Path =
      basedir.resolve(s"native$nameSuffix")

    override def artifactPath: Path = {
      val ext = if (Platform.isWindows) ".exe" else ""
      basedir.resolve(s"${compilerConfig.basename}$nameSuffix$ext")
    }

    override def toString: String = {
      val classPathFormat =
        classPath.mkString("List(", "\n".padTo(22, ' '), ")")
      s"""Config(
        | - basedir:        $basedir
        | - testConfig:     $testConfig
        | - workdir:        $workdir
        | - artifactPath:   $artifactPath
        | - logger:         $logger
        | - classPath:      $classPathFormat
        | - compilerConfig: $compilerConfig
        |)""".stripMargin
    }
  }
}
