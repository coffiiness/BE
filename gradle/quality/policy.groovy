import groovy.json.JsonOutput
import java.security.MessageDigest

def sha256 = { String text -> MessageDigest.getInstance('SHA-256').digest(text.getBytes('UTF-8')).encodeHex().toString() }
def fingerprint = { identity -> sha256(JsonOutput.toJson(new TreeMap(identity))) }
def readXml = { File file ->
    def parser = new XmlSlurper(false, false, true)
    parser.setFeature('http://xml.org/sax/features/external-general-entities', false)
    parser.setFeature('http://xml.org/sax/features/external-parameter-entities', false)
    parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
    parser.parse(file)
}
def finding = { String module, String sourceSet, Map sources, bug ->
    def clazz = bug.Class.find { it.@primary.text() == 'true' } ?: bug.Class[0]
    def method = bug.Method.find { it.@primary.text() == 'true' }
    def field = bug.Field.find { it.@primary.text() == 'true' } ?: bug.Field[0]
    def location = bug.SourceLine.find { it.@primary.text() == 'true' } ?: bug.SourceLine[0]
    if (!location || !location.@sourcepath.text()) { location = clazz.SourceLine[0] }
    String source = location.@sourcepath.text().replace('\\', '/')
    int line = location.@start.text() ? location.@start.toInteger() : 0
    File java = sources[source]
    if (!java || line < 1) { throw new IllegalStateException("Cannot identify finding source: ${module}/${source}:${line}") }
    def lines = java.readLines('UTF-8')
    if (line > lines.size()) { throw new IllegalStateException("Invalid source line: ${source}:${line}") }
    // Conservative exact location plus the normalized source statement: moving an
    // existing warning requires review instead of silently widening its allowance.
    def identity = [module: module, sourceSet: sourceSet, type: bug.@type.text(),
                    instanceHash: bug.@instanceHash.text(),
                    occurrence: bug.@instanceOccurrenceNum.text() ? bug.@instanceOccurrenceNum.toInteger() : 0,
                    className: clazz.@classname.text(),
                    method: method ? method.@name.text() + method.@signature.text() : '',
                    field: field ? field.@name.text() + ':' + field.@signature.text() : '',
                    source: source, line: line, sourceAnchor: lines[line - 1].trim()]
    if (!identity.instanceHash) { throw new IllegalStateException("Missing SpotBugs hash: ${source}:${line}") }
    [id: fingerprint(identity), identity: identity,
     module: module, sourceSet: sourceSet, type: identity.type, className: identity.className,
     source: source, line: line, priority: bug.@priority.toInteger(), rank: bug.@rank.toInteger(),
     category: bug.@category.text(), message: bug.LongMessage.text()]
}
def completeness = { xml ->
    int errors = Math.max(xml.Errors.@errors.text() ? xml.Errors.@errors.toInteger() : 0,
            xml.Errors.Error.size() + xml.Errors.AnalysisError.size())
    def missing = xml.Errors.MissingClass.collect { it.text() }
    int missingCount = Math.max(xml.Errors.@missingClasses.text() ? xml.Errors.@missingClasses.toInteger() : 0, missing.size())
    boolean valid = xml.name() == 'BugCollection' && xml.FindBugsSummary.size() == 1
    [errors: errors, missingClasses: missing, missingClassCount: missingCount,
     incomplete: !valid || errors > 0 || missingCount > 0]
}
def verify = { baseline, scan, String engine ->
    def diagnostics = []
    if (baseline.schemaVersion != 1 || baseline.engineVersion != engine ||
            baseline.javaRuntimeVersion != '17.0.21.1+2') { diagnostics << 'Baseline schema/engine/JDK version mismatch' }
    if (scan.incomplete) { diagnostics << 'Incomplete analysis (error, missing class or report)' }
    def entries = baseline.entries ?: []
    def approved = [:]
    entries.each { entry ->
        if (!entry.identity || entry.id != fingerprint(entry.identity) || approved.containsKey(entry.id)) {
            diagnostics << "Invalid or duplicate baseline identity: ${entry.id}"
        }
        def review = entry.review ?: [:]
        if (!(review.decision in ['accepted-debt', 'not-a-defect']) ||
                ['reviewId', 'reviewedBy', 'approvedBy', 'approvedAt', 'rationale', 'evidence'].any { !review[it]?.toString()?.trim() } ||
                review.rationale.toString().length() < 30 || (entry.priority as Integer) == 1) {
            diagnostics << "Unreviewed/High baseline entry: ${entry.id}"
        }
        approved[entry.id] = entry
    }
    def current = scan.findings ?: []
    if (current.groupBy { it.id }.any { id, items -> items.size() != 1 }) { diagnostics << 'Duplicate current finding identities' }
    def newFindings = current.findAll { !approved.containsKey(it.id) }
    def high = current.findAll { it.priority == 1 }
    def escalated = current.findAll { item ->
        def old = approved[item.id]
        old && (item.priority < old.priority || item.rank < old.rank)
    }
    def currentIds = current.collect { it.id }.toSet()
    def resolved = entries.findAll { !currentIds.contains(it.id) }
    [passed: diagnostics.empty && newFindings.empty && high.empty && escalated.empty,
     total: current.size(), known: current.size() - newFindings.size(), newFindings: newFindings,
     highFindings: high, escalatedFindings: escalated, resolvedEntries: resolved, diagnostics: diagnostics]
}
return [readXml: readXml, finding: finding, fingerprint: fingerprint, completeness: completeness, verify: verify]

