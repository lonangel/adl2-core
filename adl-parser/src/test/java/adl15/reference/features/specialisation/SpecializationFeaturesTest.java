/*
 * Copyright (C) 2014 Marand
 */

package adl15.reference.features.specialisation;

import com.marand.thinkehr.adl.am.AmQuery;
import com.marand.thinkehr.adl.am.mixin.AmMixins;
import com.marand.thinkehr.adl.util.TestAdlParser;
import org.openehr.jaxb.am.*;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.marand.thinkehr.adl.rm.RmObjectFactory.newIntervalOfReal;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Marko Pipan
 */
public class SpecializationFeaturesTest extends AbstractSpecializationTest {
    @Test
    public void testListConstrained() {
        FlatArchetype archetype = getArchetype("openEHR-EHR-EVALUATION.code_list_constrained.v1");
        CTerminologyCode cr = AmQuery.get(archetype, "/data[id2]/items[id3]/value/defining_code");
        assertThat(cr.getCodeList().get(0)).isEqualTo("ac1.1");
    }

    // should be updated one openEHR-EHR-OBSERVATION.spec_test_parent.v1 is fixed
    @Test
    public void testNodeOrder() {
        FlatArchetype archetype = getArchetype("openEHR-EHR-OBSERVATION.ordering_parent-merge_children.v1");
        CComplexObject cobj = AmQuery.get(archetype, "/data[id2]/events[id3]/data[id4]");
        CAttribute citems = cobj.getAttributes().get(0);

        List<String> nodeids = new ArrayList<>();
        for (CObject cObject : citems.getChildren()) {
            nodeids.add(cObject.getNodeId());
        }

        assertThat(nodeids).containsExactly("id6", "id7", "id8", "id9", "id10", "id11", "id0.2", "id12", "id13", "id10.1", "id10.2", "id0.1");

    }

    @Test
    public void testExistenceOrOccurrence0ExcludesNode() {
        FlatArchetype archetype = getArchetype("openEHR-EHR-OBSERVATION.exist_occ_0");
        assertThat(AmQuery.find(archetype, "/data[id3]/events[id4]/data")).isNotNull();
        // this attribute must be removed
        assertThat(AmQuery.find(archetype, "/data[id3]/events[id4]/state")).isNull();

        // assert allow_archetype type is removed
        CComplexObject cobj = AmQuery.get(archetype, "/protocol[id21]");
        assertThat(cobj.getAttributes()).hasSize(1);
        assertThat(cobj.getAttributes().get(0).getRmAttributeName()).isEqualTo("items");
        assertThat(cobj.getAttributes().get(0).getChildren()).hasSize(1);
        assertThat(cobj.getAttributes().get(0).getChildren().get(0).getNodeId()).isEqualTo("id22");
    }

    @Test
    public void testNarrowDvQuantity() {
        FlatArchetype archetype = getArchetype("openEHR-EHR-OBSERVATION.narrow_dv_quantity.v1");

        CComplexObject cq = AmQuery.get(archetype, "/data[id3]/events[id4]/data[id2]/items[id5]/value");
        assertThat(cq.getAttributes()).hasSize(1);
        assertThat(cq.getAttributes().get(0).getRmAttributeName()).isEqualTo("property");

        assertThat(cq.getAttributeTuples()).hasSize(1);
        final CAttributeTuple ctuple = cq.getAttributeTuples().get(0);
        assertThat(ctuple.getMembers()).hasSize(2);
        assertThat(ctuple.getMembers().get(0).getRmAttributeName()).isEqualTo("units");
        assertThat(ctuple.getMembers().get(1).getRmAttributeName()).isEqualTo("precision");
        assertThat(ctuple.getChildren()).hasSize(1);
        final CObjectTuple cObjectTuple = ctuple.getChildren().get(0);
        assertThat(cObjectTuple.getMembers()).hasSize(2);
        assertThat(((CString) cObjectTuple.getMembers().get(0)).getDefaultValue()).isEqualTo("°C");
        assertThat(((CInteger) cObjectTuple.getMembers().get(1)).getDefaultValue()).isEqualTo(1);
    }


    @Test
    public void testTupleRedefineToSingle() {
        FlatArchetype archetype = getArchetype("openEHR-EHR-OBSERVATION.tuple_redefine_to_single.v1");
        CComplexObject cobj = AmQuery.get(archetype, "/data[id2]/events[id3]/data[id4]/items[id6]/value");
        assertThat(cobj.getAttributeTuples()).hasSize(1);
        final CAttributeTuple tuple = cobj.getAttributeTuples().get(0);
        assertThat(tuple.getMembers()).hasSize(3);
        assertThat(tuple.getChildren()).hasSize(1);
        CReal cr = (CReal)(tuple.getChildren().get(0).getMembers().get(0));
        assertThat(AmMixins.of(cr.getRange()).isEqualTo(newIntervalOfReal(0.0, null))).isTrue();
        CString cs = (CString)(tuple.getChildren().get(0).getMembers().get(1));
        assertThat(cs.getDefaultValue()).isEqualTo("cm[H20]");
        CInteger ci = (CInteger)(tuple.getChildren().get(0).getMembers().get(2));
        assertThat(ci.getDefaultValue()).isEqualTo(2);
    }


    // need to implement this particular rm model (WHOLE)
    @Test(enabled = false)
    public void testDateTimeSpecializations() {
        DifferentialArchetype source = TestAdlParser.parseAdl("adl15/reference/features/specialisation/openEHR-TEST_PKG-WHOLE.date_time_specialisations.v1.adls");
        DifferentialArchetype parent = TestAdlParser.parseAdl("adl15/reference/features/basic/openEHR-TEST_PKG-WHOLE.basic_types.v1.adls");
        FlatArchetype flatParent = FLATTENER.flatten(null, parent);
        FlatArchetype archetype = FLATTENER.flatten(flatParent, source);
    }



}