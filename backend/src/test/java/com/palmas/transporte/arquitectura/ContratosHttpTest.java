package com.palmas.transporte.arquitectura;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

class ContratosHttpTest {
 @Test void conservaLosMapeosRevisados()throws Exception{
  List<String> actual=new ArrayList<>();
  Pattern patron=Pattern.compile("@(Get|Post|Put|Patch|Delete|Request)Mapping\\s*\\([^)]*\\)");
  try(var archivos=Files.walk(Path.of("src/main/java"))){
   for(var p:archivos.filter(x->x.toString().endsWith(".java")).toList()){
    var m=patron.matcher(Files.readString(p));while(m.find())actual.add(p.getFileName().toString().replace(".java","")+"|"+m.group().replaceAll("\\s+"," "));
   }
  }
  Collections.sort(actual);
  assertEquals(Files.readAllLines(Path.of("src/test/resources/arquitectura/contratos-http.txt")),actual,"Cambiar rutas exige una revisión explícita del contrato HTTP.");
 }
}
