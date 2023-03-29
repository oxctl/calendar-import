import React from 'react'

// External imports
import PropTypes from 'prop-types'

// Instructure UI Imports
import { EmotionThemeProvider } from '@instructure/emotion'
import { canvas, canvasHighContrast } from '@instructure/ui-themes'

/**
 * This attempts to load the theme from the supplied URL and then applies the theme to all the children.
 */
class LtiApplyTheme extends React.Component {

    static propTypes = {
        /**
         * The URL to load the theme variables from.
         */
        url: PropTypes.string,
        /**
         * If true then use the high contrast version of the theme.
         */
        highContrast: PropTypes.bool,
        /**
         * The content to apply the theme to.
         */
        children: PropTypes.node.isRequired
    }

    static defaultProps = {
        url: null
    }

    loading = false
    state = {
        theme: {}
    }

    componentDidMount() {
        this.loadTheme()
    }

    loadTheme = () => {
        if (!this.loading) {
            if (this.props.url) {
                this.loading = true
                fetch(this.props.url)
                    .then(response => response.json())
                    .then((json) => {
                        // Apply the loaded theme.
                        let newTheme = this.props.highContrast ? canvasHighContrast : { ...canvas, ...json }
                        this.setState({
                            theme: newTheme
                        })
                    }).finally(() => this.loading = false)
            }
        }
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (this.props.url !== prevProps.url) {
            this.loadTheme()
        }
    }

    render() {
        return (
            <EmotionThemeProvider theme={this.state.theme}>
                {this.props.children}
            </EmotionThemeProvider>
        )
    }
}

export default LtiApplyTheme