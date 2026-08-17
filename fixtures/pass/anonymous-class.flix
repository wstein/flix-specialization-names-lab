// Anonymous Java class: two Java anonymous classes instantiated within one def. See
// docs/adr/0001-content-addressed-specialization-names.md, "The five id-minting
// families" -- this is the `s"$enclosing#anon$index"`-keyed family: an index counted
// within the enclosing def's own specialization, not across the program. The implemented
// method cannot be named `run`, since that is a Flix keyword.
import java.util.function.Supplier
import java.util.Comparator

def anonClassDemo(): String \ IO = {
    let supplier = new Supplier[String] {
        def get(_this: Supplier[String]): String = "from-anon-class"
    };
    let cmp = new Comparator[String] {
        def compare(_this: Comparator[String], a: String, b: String): Int32 =
            String.length(a) - String.length(b)
    };
    "${supplier.get()} ${cmp.compare("aa", "b")}"
}

def main(): Unit \ IO = println(anonClassDemo())
