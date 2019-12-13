package diorid.tiles.screenshot

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory

open class TransparentContainerActivity : FragmentActivity() {
    companion object {
        const val EXTRA_FRAGMENT_NAME = "fragment_name"
        const val EXTRA_ARGS = "args"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentClass = intent.getStringExtra(EXTRA_FRAGMENT_NAME)
        if (savedInstanceState == null && fragmentClass != null) {
            val args = intent.getBundleExtra(EXTRA_ARGS)
            val fragment: androidx.fragment.app.Fragment? = try {
                val fragment = FragmentFactory.loadFragmentClass(
                    classLoader, fragmentClass
                ).getConstructor().newInstance()
                args?.run {
                    classLoader = fragment.javaClass.classLoader
                    fragment.arguments = this
                }
                fragment as androidx.fragment.app.Fragment
            } catch (e: Throwable) {
                null
            }

            if (fragment is DialogFragment) {
                fragment.show(supportFragmentManager, fragmentClass)
            } else if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment, fragmentClass)
                    .commit()
            }
        }
    }

}
