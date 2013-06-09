package org.openlmis.report.mapper.lookup;

import org.apache.ibatis.annotations.Select;
import org.openlmis.report.model.dto.GeographicZone;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * e-lmis
 * Created by: Elias Muluneh
 * Date: 4/12/13
 * Time: 2:39 AM
 */
@Repository
public interface GeographicZoneReportMapper {

    @Select("SELECT id, code , name " +
            "   FROM " +
            "       geographic_zones order by name")
    List<GeographicZone> getAll();
}
