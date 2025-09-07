# Eclipse Packager™ 🚀

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.packager/packager?label=Maven%20Central)](https://search.maven.org/search?q=g:org.eclipse.packager) 
[![License](https://img.shields.io/github/license/eclipse/packager)](https://github.com/eclipse/packager/blob/master/LICENSE) 
[![Matrix](https://img.shields.io/matrix/packager:matrix.eclipse.org)](https://matrix.to/#/#packager:matrix.eclipse.org)

---

## ✨ Overview

**Eclipse Packager** lets you create Linux software packages **entirely in Java**.  
Currently, it supports:

- ✅ Read and create Debian packages (`.deb`)  
- ✅ Read, create, and sign RPM packages (`.rpm`)  

---

## 🧩 Modules

Eclipse Packager is modular. Import only what you need:

| Module | Description |
|--------|-------------|
| `core` | Core utilities and abstractions |
| `deb`  | Read and create Debian packages |
| `rpm`  | Read, create, and sign RPM packages |

**Maven dependencies:**

```xml
<!-- Debian Module -->
<dependency>
    <groupId>org.eclipse.packager</groupId>
    <artifactId>packager-deb</artifactId>
    <version>$version</version>
</dependency>

<!-- RPM Module -->
<dependency>
    <groupId>org.eclipse.packager</groupId>
    <artifactId>packager-rpm</artifactId>
    <version>$version</version>
</dependency>
