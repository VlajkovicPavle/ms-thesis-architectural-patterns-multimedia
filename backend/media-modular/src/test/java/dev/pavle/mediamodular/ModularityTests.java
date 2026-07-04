package dev.pavle.mediamodular;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  static final ApplicationModules modules = ApplicationModules.of(MediaModularApplication.class);

  @Test
  void enforcesModuleBoundaries() {
    modules.verify();
  }

  @Test
  void rendersModuleDiagrams() {
    new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
  }
}
