package app.honguyen.forge.data.model

/**
 * App-wide settings. Every field is total and non-null: the mapping layer resolves the
 * proto's "unset" representations here, so nothing downstream has to handle a missing
 * value.
 */
data class ForgeSettings(
    val theme: Theme = Theme.Dark,
)
