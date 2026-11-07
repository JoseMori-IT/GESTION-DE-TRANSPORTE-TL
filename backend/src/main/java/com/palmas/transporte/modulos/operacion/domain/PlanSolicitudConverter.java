package com.palmas.transporte.modulos.operacion.domain;

import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.json.JsonMapper;
@Converter
public class PlanSolicitudConverter implements AttributeConverter<PlanViajeRequest,String> {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public String convertToDatabaseColumn(PlanViajeRequest p){return p==null?null:JSON.writeValueAsString(p);}
    public PlanViajeRequest convertToEntityAttribute(String s){return s==null?null:JSON.readValue(s,PlanViajeRequest.class);}
}
