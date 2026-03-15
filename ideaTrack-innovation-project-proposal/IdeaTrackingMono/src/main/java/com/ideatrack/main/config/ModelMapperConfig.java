package com.ideatrack.main.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.*;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mm = new ModelMapper();
        mm.getConfiguration()
          .setMatchingStrategy(MatchingStrategies.STRICT)
          .setSkipNullEnabled(true); // crucial for PATCH-like updates
        return mm;
    }
}
