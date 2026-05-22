# Create database ragchunk on local PostgreSQL (superuser)
param(
    [string]$DbHost = "localhost",
    [int]$Port = 5432,
    [string]$SuperUser = "postgres",
    [string]$SuperPassword = "123",
    [string]$DatabaseName = "ragchunk"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mvn = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd"
if (-not (Test-Path $mvn)) { $mvn = "mvn" }

$pgJar = Join-Path $root "target\deps\postgresql-42.7.10.jar"
if (-not (Test-Path $pgJar)) {
    Push-Location $root
    & $mvn -q dependency:copy-dependencies -DincludeArtifactIds=postgresql -DoutputDirectory=target/deps
    Pop-Location
}

$java = @"
import java.sql.*;
public class InitDb {
  public static void main(String[] a) throws Exception {
    Class.forName("org.postgresql.Driver");
    String url = "jdbc:postgresql://$DbHost`:$Port/postgres";
    try (Connection c = DriverManager.getConnection(url, "$SuperUser", "$SuperPassword")) {
      try (Statement s = c.createStatement()) {
        var rs = s.executeQuery("SELECT 1 FROM pg_database WHERE datname = '$DatabaseName'");
        if (rs.next()) { System.out.println("DB_EXISTS"); return; }
      }
      try (Statement s = c.createStatement()) {
        s.executeUpdate("CREATE DATABASE $DatabaseName ENCODING 'UTF8'");
        System.out.println("DB_CREATED");
      }
    }
  }
}
"@
$java | Out-File "$root\target\InitDb.java" -Encoding ascii
javac -cp $pgJar "$root\target\InitDb.java"
java -cp "$pgJar;$root\target" InitDb
