package app.aaps.plugins.source

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.profile.ProfileUtil
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.shared.tests.TestBase
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

class PoctechPluginTest : TestBase() {

    private lateinit var poctechPlugin: PoctechPlugin

    @Mock lateinit var rh: ResourceHelper
    @Mock lateinit var preferences: Preferences
    @Mock lateinit var config: Config
    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var dateUtil: DateUtil
    @Mock lateinit var profileUtil: ProfileUtil

    @BeforeEach
    fun setup() {
        poctechPlugin = PoctechPlugin(rh, aapsLogger, preferences, config, persistenceLayer, dateUtil, profileUtil)
    }

    @Test fun advancedFilteringSupported() {
        assertThat(poctechPlugin.advancedFilteringSupported()).isFalse()
    }
}
