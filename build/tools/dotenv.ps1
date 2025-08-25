# tools/dotenv.ps1
# Unified .env reader function:
# - Supports `KEY=VAL` and `export KEY=VAL`
# - Skips blank lines and comments
# - Handles quoted values (single or double quotes)
# - Allows inline comments at the end of a line (space + #)
# - If the same KEY is defined multiple times, the last one takes precedence

Set-StrictMode -Version Latest

function Read-DotEnvValue {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory=$true)][string]$FilePath,
        [Parameter(Mandatory=$true)][string]$Key
    )

    if (-not (Test-Path -LiteralPath $FilePath)) { return $null }

    # Match the target key (escaped), allowing optional "export" prefix
    $pattern = '^(?:\s*export\s+)?(?<k>' + [regex]::Escape($Key) + ')\s*=\s*(?<v>.*)$'
    $value = $null

    # Read line by line for large file compatibility
    Get-Content -LiteralPath $FilePath | ForEach-Object {
        $line = $_

        # Skip blank lines and full-line comments
        if ($line -match '^\s*$') { return }
        if ($line -match '^\s*#') { return }

        if ($line -match $pattern) {
            $v = $matches['v']

            # Remove surrounding quotes if present
            $trimmed = $v.Trim()
            if ($trimmed -match '^\s*"(.*)"\s*$') {
                $v = $matches[1]
            } elseif ($trimmed -match "^\s*'(.*)'\s*$") {
                $v = $matches[1]
            } else {
                # Strip inline comments (space + # …), ignoring escaped \#
                if ($v -match '^(.*?)(?<!\\)\s+#.*$') {
                    $v = $matches[1]
                }
            }

            $value = $v.Trim()
        }
    }

    return $value
}
