import com.typesafe.sbt.license.{LicenseInfo, DepModuleInfo}

licenseReportTitle := "LICENSE-THIRDPARTY"

licenseConfigurations := Set("compile", "provided")

licenseOverrides := {
  case DepModuleInfo("io.cucumber", "gherkin-jvm-deps", _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("com.fasterxml.jackson.core", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("commons-codec", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("org.fusesource.jansi", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("org.slf4j", _, _) =>
    LicenseInfo(LicenseCategory.MIT, "MIT License", "http://www.slf4j.org/license.html")
  case DepModuleInfo("cglib", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("com.google.guava", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("commons-io", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("commons-logging", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("org.apache.commons", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("org.apache.httpcomponents", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("org.seleniumhq.selenium", _, _) =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
  case DepModuleInfo("net.java.dev.jna", _, "4.1.0") =>
    LicenseInfo(LicenseCategory.Apache, "Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
}