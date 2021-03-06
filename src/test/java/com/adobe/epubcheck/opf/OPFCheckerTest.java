/*
 * Copyright (c) 2011 Adobe Systems Incorporated
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in
 *  the Software without restriction, including without limitation the rights to
 *  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.adobe.epubcheck.opf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.adobe.epubcheck.api.EPUBProfile;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.opf.ValidationContext.ValidationContextBuilder;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.FileResourceProvider;
import com.adobe.epubcheck.util.GenericResourceProvider;
import com.adobe.epubcheck.util.Messages;
import com.adobe.epubcheck.util.ReportingLevel;
import com.adobe.epubcheck.util.URLResourceProvider;
import com.adobe.epubcheck.util.ValidationReport;
import com.adobe.epubcheck.util.outWriter;
import com.google.common.collect.Lists;

public class OPFCheckerTest
{

  private List<MessageId> expectedErrors = Lists.newLinkedList();
  private List<MessageId> expectedWarnings = Lists.newLinkedList();
  private List<MessageId> expectedFatals = Lists.newLinkedList();
  private List<MessageId> expectedUsages = Lists.newLinkedList();
  private final Messages messages = Messages.getInstance();

  public void testValidateDocument(String fileName, EPUBVersion version)
  {
    testValidateDocument(fileName, version, false);

  }

  public void testValidateDocument(String fileName, EPUBVersion version, boolean verbose)
  {
    testValidateDocument(fileName, version, EPUBProfile.DEFAULT, false, verbose);
  }
  
  public void testValidateDocument(String fileName, EPUBVersion version, boolean checkUsages,
      boolean verbose)
  {
    testValidateDocument(fileName, version, EPUBProfile.DEFAULT, checkUsages, verbose);
  }

  public void testValidateDocument(String fileName, EPUBVersion version, EPUBProfile profile)
  {
    testValidateDocument(fileName, version, profile, false, false);
  }

  public void testValidateDocument(String fileName, EPUBVersion version, EPUBProfile profile,
      boolean checkUsages, boolean verbose)
  {
    ValidationReport testReport = new ValidationReport(fileName,
        String.format(messages.get("single_file"), "opf", version.toString(),
            profile == null ? EPUBProfile.DEFAULT : profile));
    testReport.setReportingLevel(ReportingLevel.Usage);

    GenericResourceProvider resourceProvider = null;
    if (fileName.startsWith("http://") || fileName.startsWith("https://"))
    {
      resourceProvider = new URLResourceProvider(fileName);
    }
    else
    {
      String basepath = null;
      if (version == EPUBVersion.VERSION_2)
      {
        basepath = "/20/single/opf/";
      }
      else if (version == EPUBVersion.VERSION_3)
      {
        basepath = "/30/single/opf/";
      }
      try {
        URL fileURL = this.getClass().getResource(basepath + fileName);
        String filePath = fileURL != null ? new File(fileURL.toURI()).getAbsolutePath() : basepath + fileName;
        resourceProvider = new FileResourceProvider(filePath);
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Cannot find test file", e);
      }
    }

    OPFChecker opfChecker = OPFCheckerFactory.getInstance()
        .newInstance(new ValidationContextBuilder().path("test_single_opf")
            .resourceProvider(resourceProvider).report(testReport).version(version).profile(profile)
            .build());

    assert opfChecker != null;
    opfChecker.validate();

    if (verbose)
    {
      outWriter.println(testReport);
    }

    assertEquals("The error results do not match", expectedErrors, testReport.getErrorIds());
    assertEquals("The warning results do not match", expectedWarnings, testReport.getWarningIds());
    assertEquals("The fatal error results do not match", expectedFatals,
        testReport.getFatalErrorIds());
    if (checkUsages)
      assertEquals("The usages results do not match", expectedUsages, testReport.getUsageIds());
  }

  @Before
  public void setup()
  {
    expectedErrors.clear();
    expectedWarnings.clear();
    expectedFatals.clear();
    expectedUsages.clear();
  }
  

  @Test
  public void testCMTSupport()
  {
    // tests that core media types are supported without fallbacks
    testValidateDocument("valid/cmt.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCMTPreferredTypes()
  {
    // tests that preferred media types are reported as USAGE
    expectedUsages.addAll(Collections.nCopies(4, MessageId.OPF_090));
    testValidateDocument("valid/cmt-preferred.opf", EPUBVersion.VERSION_3, true, false);
  }

  @Test
  public void testBindingsIsDeprecated()
  {
    // tests that 'bindings' is reported as deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    testValidateDocument("invalid/bindings-deprecated.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testBindingsImplemHasXHTMLMediatype()
  {
    // warns about 'bindings' being deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    // tests that 'bindings' points to an XHTML implementation
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/bindings-wrongmediatype.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentValidOPFBase001()
  {
    testValidateDocument("valid/base-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentValidOPFMediaOverlay001()
  {
    testValidateDocument("valid/media-overlay-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentValidOPFMediaOverlay002()
  {
    testValidateDocument("valid/media-overlay-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentValidOPFMinimal()
  {
    testValidateDocument("valid/minimal.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidOPFDcDate1()
  {
    testValidateDocument("valid/date-1.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidOPFDcDate2()
  {
    testValidateDocument("valid/date-2.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidOPFDcDate3()
  {
    testValidateDocument("valid/date-3.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testInvalidOPFDcDate1()
  {
    Collections.addAll(expectedWarnings, MessageId.OPF_053);
    testValidateDocument("invalid/date-1.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testInvalidOPFDcDate2()
  {
    Collections.addAll(expectedWarnings, MessageId.OPF_053);
    testValidateDocument("invalid/date-2.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testInvalidOPFNullIdentifier()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/null-identifier.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testValidateDocumentValidOPFSvg()
  {
    testValidateDocument("valid/lorem-svg.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentValidOPFSvgFallback()
  {
    testValidateDocument("valid/lorem-svg-fallback.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentMalformed()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    Collections.addAll(expectedFatals, MessageId.RSC_016);
    testValidateDocument("invalid/malformed.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNoMetadataElement()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    Collections.addAll(expectedFatals, MessageId.RSC_016);
    testValidateDocument("invalid/noMetadataElement.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNoNav()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/noNav.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentInvalidMetaAbout()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/invalidMetaAbout.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNoDcNamespace()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    Collections.addAll(expectedFatals, MessageId.RSC_016);
    testValidateDocument("invalid/noDcNamespace.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentCoverImage()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_012, MessageId.RSC_005);
    testValidateDocument("invalid/cover-image.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentFallback001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/fallback-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentFallback002()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/fallback-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentIdUnique()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/id-unique.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentIdUniqueWithSpaces()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/id-unique-spaces.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testId_WithSpaces()
  {
    testValidateDocument("valid/id-spaces.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentItemref001()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_049, MessageId.RSC_005);
    testValidateDocument("invalid/itemref-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentMediaOverlay001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005);
    testValidateDocument("invalid/media-overlay-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentMediaOverlay002()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/media-overlay-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentMediaOverlayMeta001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/media-overlay-meta-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentMinlegth()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/minlength.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentModifiedSyntax()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/modified-syntax.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentRemoteXHTML()
  {
    // No error can be thrown in single-mode validation:
    // remote resources depend on publication-wide validation
    // (e.g. to check if the resource is used a font) 
    testValidateDocument("invalid/remote-xhtml.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testValidateDocumentRemoteSVG()
  {
    // No error can be thrown in single-mode validation
    // as this can possibly used for a font
    testValidateDocument("valid/remote-svg.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentModified()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/modified.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNav001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/nav-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNav002()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/nav-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentNav003()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_012, MessageId.RSC_005);
    testValidateDocument("invalid/nav-003.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentOrder()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/order.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentRefinesRelative()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/refines-relative.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentTocncx001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/tocncx-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentTocncx002()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_050, MessageId.RSC_005);
    testValidateDocument("invalid/tocncx-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentUid001()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/uid-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentUid002()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/uid-002.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testValidateDocumentItemProperties()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_012);
    testValidateDocument("invalid/item-properties.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testValidateDocumentItemNoMediaType()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/item-nomediatype.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testBadOPFNamespace20()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/bad-opf-namespace.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testBadOPFDupeID()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/dupe-id.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testOPF_Issue216()
  {
    testValidateDocument("valid/issue216.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testEmptyGuideElementIssue663_EPUB2()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/issue663_empty-guide.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testInvalidIdentifierUUIDIssue853_EPUB2()
  {
    Collections.addAll(expectedWarnings, MessageId.OPF_085);
    testValidateDocument("invalid/issue853_invalid-uuid-001.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testInvalidIdentifierUUIDAttrIssue853_EPUB2()
  {
    Collections.addAll(expectedWarnings, MessageId.OPF_085);
    testValidateDocument("invalid/issue853_invalid-uuid-002.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testInvalidIdentifierUUIDIssue853_EPUB3()
  {
    Collections.addAll(expectedWarnings, MessageId.OPF_085);
    testValidateDocument("invalid/issue853_invalid-uuid.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testFilenameInManifestContainsSpacesIssue239_EPUB2()
  {
    Collections.addAll(expectedWarnings, MessageId.PKG_010);
    testValidateDocument("invalid/filename_contains_spaces_issue239.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testFilenameInManifestContainsSpacesIssue239_EPUB3()
  {
    Collections.addAll(expectedWarnings, MessageId.PKG_010);
    testValidateDocument("invalid/filename_contains_spaces_issue239.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMultipleDCTypes()
  {
    testValidateDocument("valid/dc-type.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMultipleDCSource()
  {
    testValidateDocument("valid/dc-source.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaMetaAuthIsDeprecated()
  {
    // tests that the 'meta-auth' property is reported as deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    testValidateDocument("invalid/meta-metaauth-deprecated.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testMetaDisplaySeq()
  {
    // tests that the 'display-seq' property is allowed
    testValidateDocument("valid/meta-displayseq.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaPropertyMultipleNames()
  {
    // tests that the 'property' attribute takes only one value
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.OPF_025);
    testValidateDocument("invalid/meta-property-multiplenames.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaPropertyUndefined()
  {
    // tests that undefined properties in the default vocab are not allowed
    Collections.addAll(expectedErrors, MessageId.OPF_027);
    testValidateDocument("invalid/meta-property-undefined.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaPropertyUndefinedPrefix()
  {
    // tests that properties with an undeclared prefix are not allowed
    Collections.addAll(expectedErrors, MessageId.OPF_028);
    testValidateDocument("invalid/meta-property-undefined-prefix.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaRefinesCycle()
  {
    expectedErrors.add(MessageId.OPF_065);
    testValidateDocument("invalid/meta-refines-cycle.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSchemeAttribute()
  {
    testValidateDocument("valid/meta-scheme.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSchemeWithUndefinedValue()
  {
    // tests that the 'scheme' attribute with an undefined unprefixed value is reported
    Collections.addAll(expectedErrors, MessageId.OPF_027);
    testValidateDocument("invalid/meta-scheme-undefinedvalue.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSchemeWithMultipleNames()
  {
    // tests that the 'scheme' attribute only allows a single value 
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.OPF_025);
    testValidateDocument("invalid/meta-scheme-multiplevalues.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testMetaSourceOf()
  {
    testValidateDocument("valid/meta-source-of.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSourceOfWrongValue()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-source-of-wrongvalue.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSourceOfNoRefines()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-source-of-norefines.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSourceWrongRefinesTarget()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-source-of-wrongrefines.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSubjectAuthorityAndTerm() {
    testValidateDocument("valid/meta-subject-authority-term.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSubjectAuthorityButNoTerm() {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-subject-authority-noterm.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSubjectAuthorityMoreThanOnce() {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-subject-authority-morethanone.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSubjectTermButNoAuthority() {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-subject-term-noauthority.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSubjectTermMoreThanOnce() {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-subject-term-morethanone.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelAcquire()
  {
    // tests that the 'acquire' link rel keyword is accepted
    testValidateDocument("valid/link-rel-acquire.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelAlternate()
  {
    // tests that the 'alternate' link rel keyword is accepted
    testValidateDocument("valid/link-rel-alternate.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelAlternateCannotHaveOtherProperty()
  {
    // tests that the 'alternate' link rel keyword cannot be combined with another
    // keyword
    expectedErrors.add(MessageId.OPF_089);
    testValidateDocument("invalid/link-rel-alternate-with-other-property.opf",
        EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelMultipleProperties()
  {
    // tests that links can have multiple rel keywords (if not 'alternate')
    testValidateDocument("valid/link-rel-multiple-properties.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecord()
  {
    // tests that the 'record' link rel keyword is accepted
    testValidateDocument("valid/link-rel-record.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordRemote()
  {
    // tests that remote 'record' links are accepted
    testValidateDocument("valid/link-rel-record-remote.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordWithOtherKeyword()
  {
    // tests that the 'record' rel link keyword can be combined with another keyword
    testValidateDocument("valid/link-rel-record-with-other-keyword.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordWithIdentifierProperty()
  {
    // tests that the 'record' rel link properties are accepted
    testValidateDocument("valid/link-rel-record-identifier-properties.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordDeprecated()
  {
    // tests that deprecated rel link keywords are reported as warnings
    expectedWarnings.addAll(Collections.nCopies(5, MessageId.OPF_086));
    testValidateDocument("invalid/link-rel-record-deprecated.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordWithNoMediaType()
  {
    // tests that a 'record' link with no media type is reported as an error
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/link-rel-record-no-mediatype.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordWithUndefinedIdentifierProperty()
  {
    // tests that a 'record' link with an undefined unprefixed property is reported
    // as an error
    expectedErrors.add(MessageId.OPF_027);
    testValidateDocument("invalid/link-rel-record-undefined-identifier-property.opf",
        EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelRecordWithRefines()
  {
    // tests that a 'record' links cannot refine another resource
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/link-rel-record-refines.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testLinkRelVoicing()
  {
    // tests that the 'voicing' link rel keyword is accepted
    testValidateDocument("valid/link-rel-voicing.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkRelVoicingWithNoRefinesAttribute()
  {
    // tests that 'voicing' links must have a 'refines' attribute
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/link-rel-voicing-no-refines.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testLinkRelVoicingWithNoMediaType()
  {
    // tests that 'voicing' links must have a 'media-type' attribute
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/link-rel-voicing-no-type.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testLinkRelVoicingWithMediaTypeNotAudio()
  {
    // tests that 'voicing' links must refer to resources with an audio mime type
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/link-rel-voicing-not-audio.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLink_ResourceInManifest()
  {
    expectedErrors.add(MessageId.OPF_067);
    testValidateDocument("invalid/link-in-manifest.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testLinkPropertiesEmpty()
  {
    // tests that an empty `properties` attribute is disallowed on `link` elements
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/link-properties-empty.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaBelongsToCollection()
  {
    testValidateDocument("valid/meta-collection.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaBelongsToCollectionWrongRefines()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-collection-refine-noncollection.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaCollectionTypeNoRefines()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-collection-type-norefines.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaCollectionTypeWrongRefines()
  {
    expectedErrors.add(MessageId.RSC_005);
    testValidateDocument("invalid/meta-collection-type-refines-noncollection.opf",
        EPUBVersion.VERSION_3);
  }

  @Test
  public void testPrefixDeclaration()
  {
    testValidateDocument("valid/prefixes.opf", EPUBVersion.VERSION_3);
  } 

  @Test
  public void testPrefixDeclarationInvalidSyntax()
  {
    // tests the prefix attribute syntax errors are reported
    Collections.addAll(expectedErrors, MessageId.OPF_004c, MessageId.OPF_004c);
    testValidateDocument("invalid/prefix-declaration.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testPrefixDeclarationInvalidRedeclares()
  {
    // tests that the redeclaration of a reserved prefix raises a warning
    expectedWarnings.add(MessageId.OPF_007);
    // tests that the definition of a new prefix for a default vocab raises a
    // warning
    expectedWarnings.addAll(Collections.nCopies(4, MessageId.OPF_007b));
    testValidateDocument("invalid/prefixes-redeclare.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionPropertiesValid()
  {
    testValidateDocument("valid/rendition-properties.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionGlobalsValid()
  {
    testValidateDocument("valid/rendition-globals.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionGlobalsWithRefines()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005);
    testValidateDocument("invalid/rendition-globals-refines.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionGlobalsDuplicated()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005);
    testValidateDocument("invalid/rendition-globals-duplicate.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionGlobalsBadValues()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005);
    testValidateDocument("invalid/rendition-globals-badvalues.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionOverridesConflicts()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005, MessageId.RSC_005, MessageId.RSC_005,
        MessageId.RSC_005, MessageId.RSC_005);
    testValidateDocument("invalid/rendition-overrides-conflicts.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionSpreadPortraitIsDeprecated()
  {
    // tests that the 'portrait' value of the 'rendition:spread' property is
    // reported as deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    testValidateDocument("invalid/rendition-spread-portrait-deprecated.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionSpreadPortraitIsDeprecatedOnItemref()
  {
    // tests that the 'portrait' value of the 'rendition:spread' property is
    // reported as deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    testValidateDocument("invalid/rendition-spread-portrait-deprecated-onitemref.opf",
        EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testRenditionViewportIsDeprecated() {
    // tests that the 'rendition:viewport' property is reported as deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017, MessageId.RSC_017, MessageId.RSC_017);
    testValidateDocument("invalid/rendition-viewport-deprecated.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testRenditionViewportDuplicates() {
    // warnings raised as 'rendition:viewport' is deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017, MessageId.RSC_017);
    // tests that the 'rendition:viewport' property cannot be set twice
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/rendition-viewport-duplicates.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testRenditionViewportWithBadValue() {
    // warnings raised as 'rendition:viewport' is deprecated
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    // tests that the 'rendition:viewport' property has a conforming value
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/rendition-viewport-badvalue.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_Preview()
  {
    testValidateDocument("valid/collection-preview.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_Unknown()
  {
    expectedErrors.add(MessageId.OPF_068);
    testValidateDocument("invalid/collection-unknown-role.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_Foreign()
  {
    testValidateDocument("valid/collection-foreign.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_ForeignInvalid_idpforg()
  {
    expectedErrors.add(MessageId.OPF_069);
    testValidateDocument("invalid/collection-foreign-idpf.org.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_ForeignInvalid_BadURI()
  {
    expectedWarnings.add(MessageId.OPF_070);
    testValidateDocument("invalid/collection-foreign-invalid-uri.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_DO()
  {
    testValidateDocument("valid/collection-do.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_DOInvalid()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/collection-invalid-do-sch-001.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testCollection_ManifestInvalid_TopLevel()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/collection-manifest-toplevel.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testMetaSchemaOrg()
  {
    testValidateDocument("valid/meta-schemaorg.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testEdupub()
  {
    testValidateDocument("valid/edupub-minimal.opf", EPUBVersion.VERSION_3, EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_Teacher()
  {
    testValidateDocument("valid/edupub-teacher-edition.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_InvalidDCType()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/edupub-missing-dc-type.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_InvalidNoAccessFeature()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/edupub-no-accessFeature.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_InvalidAccessFeatureNone()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/edupub-accessFeature-none.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_InvalidTeacherNoDCType()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/edupub-teacher-edition-no-dc-type.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testEdupub_InvalidTeacherNoSource()
  {
    Collections.addAll(expectedWarnings, MessageId.RSC_017);
    testValidateDocument("invalid/edupub-teacher-edition-nosource.opf", EPUBVersion.VERSION_3,
        EPUBProfile.EDUPUB);
  }

  @Test
  public void testSC_Embedded()
  {
    testValidateDocument("valid/sc-embedded.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testSC_InvalidUndeclaredPrefix()
  {
    // verifies that the epubsc: prefix is not recognized as an reserved prefix -- pulled from epub 3.2
    Collections.addAll(expectedErrors, MessageId.OPF_028);
    testValidateDocument("invalid/sc-undeclared-prefix.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_Collection()
  {
    testValidateDocument("valid/idx-collection.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_CollectionWithSubGroup()
  {
    testValidateDocument("valid/idx-collection-indexgroup.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_CollectionInvalid_ChildNoIndexGroup()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/idx-collection-invalidchild.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_CollectionInvalid_TopLevelIndexGroup()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/idx-collection-toplevel-indexgroup.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_CollectionInvalid_IndexGroupWithChild()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/idx-collection-indexgroup-withchild.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testIDX_CollectionInvalid_ResourceNotContentDoc()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_071, MessageId.OPF_071);
    testValidateDocument("invalid/idx-collection-resource-noxhtml.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testGuideReferenceUnique_EPUB2()
  {
    Collections.addAll(expectedWarnings, MessageId.RSC_017, MessageId.RSC_017);
    testValidateDocument("invalid/guide-duplicates.opf", EPUBVersion.VERSION_2);
  }

  @Test
  public void testGuideReferenceUnique_EPUB3()
  {
    Collections.addAll(expectedWarnings, MessageId.RSC_017, MessageId.RSC_017);
    testValidateDocument("invalid/guide-duplicates.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testDict_Single()
  {
    testValidateDocument("valid/dict-single.opf", EPUBVersion.VERSION_3, EPUBProfile.DICT);
  }

  @Test
  public void testDict_Single_NoSKM()
  {
    // No SKM Doc
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-single-noskm.opf", EPUBVersion.VERSION_3, EPUBProfile.DICT);
  }

  @Test
  public void testDict_Single_NoSKM_2()
  {
    // SKM doc missing a required property
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-single-noskm-2.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple()
  {
    testValidateDocument("valid/dict-multiple.opf", EPUBVersion.VERSION_3, EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple_NoSKM()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_083);
    testValidateDocument("invalid/dict-multiple-noskm.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple_CollectionWithMultipleSKMs()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_082);
    testValidateDocument("invalid/dict-multiple-morethanoneskm.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple_CollectionSharingSKM()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-multiple-sharedskm.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple_CollectionContainingMissingResource()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_081);
    testValidateDocument("invalid/dict-multiple-missingresource.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }
  
  @Test
  public void testDict_Multiple_CollectionContainingNonXHTML()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_084);
    testValidateDocument("invalid/dict-multiple-nonxhtml.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Multiple_CollectionWithSubCollection()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-multiple-subcollection.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_NoDCType()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-nodctype.opf", EPUBVersion.VERSION_3, EPUBProfile.DICT);
  }

  @Test
  public void testDict_SKM_BadMediaType()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_012);
    testValidateDocument("invalid/dict-skm-badmediatype.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_InCollection()
  {
    testValidateDocument("valid/dict-multiple-dedicatedlang.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_InvalidInCollection()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-invalidcollection.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_InvalidInTopLevel()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-invalidtoplevel.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_MissingInCollection()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-missingcollection.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_MissingTopLevel()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-missingtoplevel.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_UndeclaredInCollection()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-undeclaredcollection.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testDict_Lang_UndeclaredTopLevel()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-lang-undeclaredtoplevel.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }
  
  @Test
  public void testDict_Type()
  {
    testValidateDocument("valid/dict-single-typed.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }
  
  @Test
  public void testDict_Type_Unknown()
  {
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/dict-type-unknown.opf", EPUBVersion.VERSION_3,
        EPUBProfile.DICT);
  }

  @Test
  public void testManifest_DuplicateResource()
  {
    Collections.addAll(expectedErrors, MessageId.OPF_074);
    testValidateDocument("invalid/manifest-duplicate-resource.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testItemrefDuplicate() {
    // tests that duplicate itemref references are not allowed
    Collections.addAll(expectedErrors, MessageId.RSC_005);
    testValidateDocument("invalid/itemref-duplicate.opf", EPUBVersion.VERSION_3);
  }

  @Test
  public void testVocabA11y() {
    // tests that the a11y vocb and known properties are allowed
    testValidateDocument("valid/vocab-a11y-declared.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testVocabA11yUndeclared() {
    // tests that the a11y prefix is predefined
    testValidateDocument("valid/vocab-a11y-undeclared.opf", EPUBVersion.VERSION_3);
  }
  
  @Test
  public void testVocabA11yUnknownProperty() {
    // tests that properties in the a11y vocab are checked
    // expects 1 error for a `meta` property, and 1 error for a `link/@rel` property
    Collections.addAll(expectedErrors, MessageId.OPF_027, MessageId.OPF_027);
    testValidateDocument("invalid/vocab-a11y-unknownproperty.opf", EPUBVersion.VERSION_3);
  }
  

}
