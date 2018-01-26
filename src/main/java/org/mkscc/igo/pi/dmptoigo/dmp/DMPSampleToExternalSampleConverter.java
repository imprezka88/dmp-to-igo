package org.mkscc.igo.pi.dmptoigo.dmp;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mkscc.igo.pi.dmptoigo.cmo.CMOSampleIdResolver;
import org.mkscc.igo.pi.dmptoigo.cmo.MrnToCmoPatientIdResolver;
import org.mkscc.igo.pi.dmptoigo.dmp.domain.DMPSample;
import org.mkscc.igo.pi.dmptoigo.dmp.domain.DMPTumorNormal;
import org.mkscc.igo.pi.dmptoigo.dmp.domain.SampleType;
import org.mskcc.domain.sample.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DMPSampleToExternalSampleConverter {
    private static final Logger LOGGER = LogManager.getLogger(DMPSampleToExternalSampleConverter.class);

    public static final SampleOrigin DEFAULT_NORMAL_SAMPLE_ORIGIN = SampleOrigin.WHOLE_BLOOD;
    public static final SampleOrigin DEFAULT_TUMOR_SAMPLE_ORIGIN = SampleOrigin.TISSUE;
    public static final NucleicAcid DEFAULT_NUCLEID_ACID = NucleicAcid.DNA;
    public static final SpecimenType DEFAULT_NORMAL_SPECIMEN_TYPE = SpecimenType.BLOOD;
    public static final SpecimenType DEFAULT_TUMOR_SPECIMEN_TYPE = SpecimenType.BIOPSY;
    public static final SampleClass DEFAULT_NORMAL_SAMPLE_CLASS = SampleClass.NORMAL;

    @Autowired
    private CMOSampleIdResolver cmoSampleIdResolver;

    @Autowired
    private DmpPatientId2CMOPatientIdRepository dmpPatientId2CMOPatientIdRepository;

    public ExternalSample convert(DMPSample dmpSample) {
        LOGGER.info(String.format("Converting dmp sample: %s to External Sample", dmpSample));

        ExternalSample externalSample = new ExternalSample(
                dmpSample.getRunID(),
                dmpSample.getBamPath(),
                dmpSample.getDmpId(),
                dmpSample.getPatientDmpId());

        TumorNormalType tumorNormal = getTumorNormal(dmpSample.getTumorNormal());

        externalSample.setNucleidAcid(DEFAULT_NUCLEID_ACID.getValue());
        externalSample.setPatientCmoId(getCmoPatientId(dmpSample.getPatientDmpId()));
        externalSample.setSampleClass(getSampleClass(tumorNormal, dmpSample.getSampleType()));
        externalSample.setSampleOrigin(getSampleOrigin(tumorNormal));
        externalSample.setSpecimenType(getSpecimenType(tumorNormal));
        externalSample.setTumorNormal(tumorNormal.getValue());
        externalSample.setCounter(dmpSample.getCounter());

        externalSample.setCmoId(cmoSampleIdResolver.resolve(externalSample));

        LOGGER.info(String.format("Dmp Sample %s converted to External Sample: %s", dmpSample.getDmpId(), externalSample));

        return externalSample;
    }

    private String getSampleClass(TumorNormalType tumorNormalType, String sampleType) {
        if(tumorNormalType == TumorNormalType.NORMAL)
            return DEFAULT_NORMAL_SAMPLE_CLASS.getValue();

        if(StringUtils.isEmpty(sampleType))
            return "";
        return SampleType.getByValue(sampleType).getSampleClass().getValue();
    }

    private String getSpecimenType(TumorNormalType tumorNormal) {
        return tumorNormal == TumorNormalType.NORMAL ? DEFAULT_NORMAL_SPECIMEN_TYPE.getValue() : DEFAULT_TUMOR_SPECIMEN_TYPE.getValue();
    }

    private String getSampleOrigin(TumorNormalType tumorNormal) {
        return tumorNormal == TumorNormalType.NORMAL ? DEFAULT_NORMAL_SAMPLE_ORIGIN.getValue() : DEFAULT_TUMOR_SAMPLE_ORIGIN.getValue();
    }


    private String getCmoPatientId(String dmpPatientId) {
        return dmpPatientId2CMOPatientIdRepository.getCMOPatientIdByDMPPatientId(dmpPatientId);
    }

    private TumorNormalType getTumorNormal(String tumorNormal) {
        return DMPTumorNormal.getByValue(tumorNormal).getIgoValue();
    }
}
