import re

with open(r'D:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\server\services\V2SubmitFeatureFlags.java', 'r') as f:
    content = f.read()

methods = re.findall(r'public static boolean (is\w+Enabled)\(\)', content)

test_code = '''package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2SubmitFeatureFlagsTest {

    @BeforeEach
    @AfterEach
    public void tearDown() {
'''

for m in methods:
    # infer property name
    # e.g. isSubmitDryRunValidationEnabled -> tse.v2.submitDryRunValidation.enabled
    prop = m[2].lower() + m[3:-7] + '.enabled'
    test_code += f'        System.clearProperty(\"tse.v2.{prop}\");\n'

test_code += '''    }

    @Test
    public void testDefaultIsFalse() {
'''

for m in methods:
    test_code += f'        assertFalse(V2SubmitFeatureFlags.{m}());\n'

test_code += '''    }

    @Test
    public void testSetPropertyTrue() {
'''

for m in methods:
    prop = m[2].lower() + m[3:-7] + '.enabled'
    test_code += f'        System.setProperty(\"tse.v2.{prop}\", \"true\");\n'
    test_code += f'        assertTrue(V2SubmitFeatureFlags.{m}());\n'

test_code += '''    }

    @Test
    public void testSetPropertyFalse() {
'''

for m in methods:
    prop = m[2].lower() + m[3:-7] + '.enabled'
    test_code += f'        System.setProperty(\"tse.v2.{prop}\", \"false\");\n'
    test_code += f'        assertFalse(V2SubmitFeatureFlags.{m}());\n'

test_code += '''    }
}
'''

with open(r'D:\Ban_sao_du_an\src\test\java\com\mycompany\tutorhub_enterprise\server\services\V2SubmitFeatureFlagsTest.java', 'w') as f:
    f.write(test_code)
